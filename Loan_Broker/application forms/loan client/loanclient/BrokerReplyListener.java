import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import javax.swing.*;
import java.io.Serializable;

public class BrokerReplyListener implements MessageListener {
    private Session session;
    private boolean transacted = false;
    private MessageProducer replyProducer;

    private static final int ackMode;

    private LoanClientFrame lcf = null;

    private static final String messageBrokerUrl;

    private static final String messageQueueName;

    static {
        messageBrokerUrl = "tcp://localhost:61616";
        messageQueueName = "LoanRequestReplyQueue";
        ackMode = Session.AUTO_ACKNOWLEDGE;
    }

    private String correlationID = "BrokerReplyListener";

    public LoanClientFrame getLcf() {
        return lcf;
    }

    public void setLcf(LoanClientFrame lcf) {
        this.lcf = lcf;
    }

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
            if (message instanceof ObjectMessage) {
                System.out.print("\n I got your BrokerReply! The BrokerReply was: " + message.toString());
                //add reply to list
                RequestReply<BankInterestRequest, BankInterestReply> rr = (RequestReply<BankInterestRequest, BankInterestReply>)((ObjectMessage) message).getObject();


                LoanRequest lr = new LoanRequest();
                lr.setAmount(rr.getRequest().getAmount());
                lr.setTime(rr.getRequest().getTime());

                lcf.add(lr, rr.getReply());
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