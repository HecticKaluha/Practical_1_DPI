import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import javax.swing.*;
import java.io.Serializable;

public class LoanRequestListener implements MessageListener {
    private Session session;
    private boolean transacted = false;
    private MessageProducer replyProducer;

    private static final int ackMode;

    private static final String messageBrokerUrl;

    private static final String messageQueueName;
    private JScrollPane lbf = null;

    static {
        messageBrokerUrl = "tcp://localhost:61616";
        messageQueueName = "LoanRequestQueue";
        ackMode = Session.AUTO_ACKNOWLEDGE;
    }

    private String correlationID = "Middle Man";

    public LoanRequestListener() {
        /*try {
            //This message broker is embedded
            //BrokerService broker = new BrokerService();
            broker.setPersistent(false);
            broker.setUseJmx(false);
            broker.addConnector(messageBrokerUrl);
            broker.start();
        } catch (Exception e) {
            //Handle the exception appropriately
        }*/
    }

    public void setupMessageQueueConsumer() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(messageBrokerUrl);
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            this.session = connection.createSession(this.transacted, ackMode);
            Destination adminQueue = this.session.createQueue(messageQueueName);


            this.replyProducer = this.session.createProducer(null);
            this.replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);


            MessageConsumer consumer = this.session.createConsumer(adminQueue);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            System.out.print("Something went wrong: " + e.getMessage());
        }
    }

    public synchronized void onException(JMSException ex) {
        System.out.println("JMS Exception occured.  Shutting down client.");
    }

    @Override
    public void onMessage(Message message) {
        try {
            TextMessage response = this.session.createTextMessage();
            if (message instanceof ObjectMessage) {
                System.out.print("I got your message! The message was: " + message.toString());
                response.setText("OK");
                //send request to bank
                sendRequestToBank(((ObjectMessage) message).getObject());

            }
            else{
                System.out.print("Something went wrong while de-enqueueing the message");
            }






            //respond only when you received reply from bank
            response.setJMSCorrelationID(message.getJMSCorrelationID());
            this.replyProducer.send(message.getJMSReplyTo(), response);
        } catch (JMSException e) {
            System.out.print("Something went wrong: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        new LoanRequestListener();
    }

    public void sendRequestToBank(Serializable loanRequest)
    {
        LoanRequest lr = (LoanRequest)loanRequest;
        Session session = null;
        Connection connection = null;
        try
        {
            connection = ConnectionManager.getNewConnection();
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue("BankLoanRequestQueue");

            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);


            ObjectMessage message = session.createObjectMessage(lr);
            Destination replyDestination = session.createQueue("BankLoanRequestReplyQueue");

            message.setJMSReplyTo(replyDestination);
            message.setJMSCorrelationID(correlationID);

            System.out.println("Sending message: "+ lr.toString() + " : " + Thread.currentThread().getName());
            producer.send(message);
            System.out.println("Sent message: "+ lr.toString() + " : " + Thread.currentThread().getName());
            session.close();
            connection.close();
        }
        catch(CouldNotCreateConnectionException | JMSException e)
        {
            System.out.print(e.getMessage());
        }
        finally {
            try{
                if (session != null && connection != null) {
                    session.close();
                    connection.close();
                }
            }
            catch(JMSException e)
            {
                System.out.print(e.getMessage());
            }
        }
    }
}