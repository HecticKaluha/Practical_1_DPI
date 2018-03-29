import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import javax.swing.*;
import java.io.Serializable;

public class LoanRequestListener implements MessageListener {
    private Session session;
    private boolean transacted = false;
    private MessageProducer replyProducer;



    private LoanBrokerFrame brokerFrame = null;

    private static final int ackMode;

    private static final String messageBrokerUrl;

    private static final String messageQueueName;

    static {
        messageBrokerUrl = "tcp://localhost:61616";
        messageQueueName = "LoanRequestQueue";
        ackMode = Session.AUTO_ACKNOWLEDGE;
    }

    private String correlationID = "LoanRequestListener";

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

    public LoanBrokerFrame getBrokerFrame() {
        return brokerFrame;
    }

    public void setBrokerFrame(LoanBrokerFrame brokerFrame) {
        this.brokerFrame = brokerFrame;
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
            System.out.print("\n Something went wrong: " + e.getMessage());
        }
    }

    public synchronized void onException(JMSException ex) {
        System.out.println("\n JMS Exception occured.  Shutting down client.");
    }

    @Override
    public void onMessage(Message message) {
        try {
            TextMessage response = this.session.createTextMessage();
            if (message instanceof ObjectMessage) {
                System.out.print("\n I got your Loanrequest! The Request was: " + message.toString());
                response.setText("\n OK");

                LoanRequest lr = (LoanRequest)((ObjectMessage) message).getObject();

                brokerFrame.add(lr);
                //send request to bank
                sendRequestToBank(((ObjectMessage) message).getObject());

            }
            else{
                System.out.print("\n Something went wrong while de-enqueueing the message");
            }






            //respond only when you received reply from bank
            //response.setJMSCorrelationID(message.getJMSCorrelationID());
            //this.replyProducer.send(message.getJMSReplyTo(), response);
        } catch (JMSException e) {
            System.out.print("\n Something went wrong: " + e.getMessage());
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

            //TODO: send bank interest
            BankInterestRequest bir = new BankInterestRequest(lr.getAmount(), lr.getTime());
            ObjectMessage message = session.createObjectMessage(bir);
            Destination replyDestination = session.createQueue("BankLoanRequestReplyQueue");

            message.setJMSReplyTo(replyDestination);
            message.setJMSCorrelationID(correlationID);

            System.out.println("\n Sending Client Loanrequest to bank: "+ lr.toString() + " : " + Thread.currentThread().getName());
            brokerFrame.add(lr, bir);
            producer.send(message);
            System.out.println("\n Sent message: "+ lr.toString() + " : " + Thread.currentThread().getName());
            session.close();
            connection.close();
        }
        catch(CouldNotCreateConnectionException | JMSException e)
        {
            System.out.print("\n" + e.getMessage());
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
                System.out.print("\n" + e.getMessage());
            }
        }
    }
}