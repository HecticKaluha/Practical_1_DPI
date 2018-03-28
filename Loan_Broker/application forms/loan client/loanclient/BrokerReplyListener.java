import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import javax.swing.*;
import java.io.Serializable;

public class BrokerReplyListener implements MessageListener {
    private Session session;
    private boolean transacted = false;
    private MessageProducer replyProducer;

    private static final int ackMode;

    private static final String messageBrokerUrl;

    private static final String messageQueueName;
    private JScrollPane lbf = null;

    static {
        messageBrokerUrl = "tcp://localhost:61616";
        messageQueueName = "LoanRequestReplyQueue";
        ackMode = Session.AUTO_ACKNOWLEDGE;
    }

    private String correlationID = "BrokerReplyListener";

    public BrokerReplyListener() {
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
                System.out.print("\n I got your BrokerReply! The BrokerReply was: " + message.toString());
                response.setText("\n OK");
                //add reply to list
            }
            else if (message instanceof TextMessage)
            {
                System.out.print("\n" + ((TextMessage) message).getText());
            }
            else{
                System.out.print("\n Something went wrong while de-enqueueing the message");
            }
        } catch (JMSException e) {
            System.out.print("\n Something went wrong: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        new BrokerReplyListener();
    }

}