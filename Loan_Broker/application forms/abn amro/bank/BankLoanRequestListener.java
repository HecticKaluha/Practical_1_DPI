import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import javax.swing.*;
import java.io.Serializable;

public class BankLoanRequestListener implements MessageListener {
    private Session session;
    private boolean transacted = false;
    private MessageProducer replyProducer;

    private static final int ackMode;

    private static final String messageBrokerUrl;

    private static final String messageQueueName;

    private ObjectMessage response =null;

    static {
        messageBrokerUrl = "tcp://localhost:61616";
        messageQueueName = "BankLoanRequestQueue";
        ackMode = Session.AUTO_ACKNOWLEDGE;
    }

    private String correlationID = "BankLoanRequestListener";

    public BankLoanRequestListener() {
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
            System.out.print("\n Something went wrong while setting up a connection: " + e.getMessage());
        }
    }

    public synchronized void onException(JMSException ex) {
        System.out.println("\n JMS Exception occured.  Shutting down client.");
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof ObjectMessage) {
            System.out.print("\n I got your BrokerLoanRequest! The BrokerLoanRequest was: " + message.toString());
            //TODO: set message to a line and add to scrollpane.
        }
        else{
            System.out.print("\n Something went wrong while de-enqueueing the message");
        }
    }


    public static void main(String[] args) {
        new BankLoanRequestListener();
    }

    public void sendResponse(Serializable bankreply) {

        try {
            response = this.session.createObjectMessage(bankreply);
            //respond only when bank filled in form
            //TODO: get correlationID from message from selectedline in scrollpane
            response.setJMSCorrelationID("BankReplyListener");
            //TODO: get replydestination from message from selected line in scrollpane
            System.out.print("\n Sending Bankinterestreply to broker: " + response.toString());
            Destination replyDestination = session.createQueue("BankLoanRequestReplyQueue");
            this.replyProducer.send(replyDestination, response);
        } catch (JMSException e) {
            System.out.print("\n" + e.getMessage());
        }
    }
}