**xmail-java** - Java 1.8 MDA delivery library.

# Description
This small project aims to demonstrate how to properly deliver emails according to RFC standards and adaptive enough to accommodate non standard MTA's.

# How it works
1. The Composer class is used to compose and add an email to the queue.

2. A queue service that is contained within this library will pick any emails in the queue and try to deliver them.
  1. The queue will attempt to deliver only when it has enough free slots to open a new thread
  2. If it is the first attempt to deliver an email it will choose the next available IP and a next available MX for delivery in order to balance the load between outbound IP's and recipient MX servers at high load.
    1. If the email was queued before, it will check what wore the errors or previous deliveries and try to adapt to avoid them if possible.
    2. In case of failure it will not just try to adapt itself but also pick another MX server for the recipient domain if available.
3. By default it will attempt to use **ESMTP** but with fall back to **SMTP** in case of error.
  1. In case of ESMTP and **TLS** support it will attempt to secure the connection.
  2. If a failure occurs the email will be re-queued and the next attempt will not try to STARTTLS.
4. If the MTA has advertised **SIZE** it will check if the email size is allowed by server.
  1. If not, it will cancel the sending and it will send the bounce message.
  2. If the email does not exceed the advertised size the MAIL command will be extended with the SIZE parameter to announce what size of email it wants to deliver.

5. After sending, it will check the response received from server. If the email could not be delivered, it will check if it was a connection problem, or a temporary or permanent error from MTA. If the error was just temporary (connection or received from MTA), the email will be queued
6. If the limit of attempts was reached, the email will be deleted from the queue and a bounce message will be sent.
7. If there was a permanent error or no MX's wore found or a connection to the MTA was not established the email will not be queued and the bounce message will be sent.
8. The thread will end here and it will notify the main thread about this.
9. If the **SIGTERM** or **SIGINT** will be received from the main thread, all the threads will be notified about this and the main process will stop after all current running jobs are gracefully finished.

# Highlits

1. [Composer Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/SMTP/Composer.java)
  The email composer. It takes the recipient address, subject, message, any attachments and also accepts some headers.
  It supports MIME format and it assumes that the message contains HTML markups. It will create also the text/plain of the message for compliance.
  It also contains a method for creating the bounce messages which will be used to notify the sender in case the email delivery failed.

2. [Sender Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/SMTP/Sender.java)
  The queue service.
  It connects to the remote MTA and tries to deliver.
  It defaults to ESMTP and sends EHLO and if STARTTLS advertised will also encrypt the connection.
  If EHLO is not supported it will default to SMTP protocol and send HELO command.
  It also checks the SIZE if advertised and sends it's own with the MAIL command to try to avoid any unnecessary traffic if the emails is too large.
  This is a basic implementation to demonstrate how things should work.

3. [AdvancedSender Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/SMTP/AdvancedSender.java)
  This is  an extension of the Sender Class and comes with Round Robin MX cycling, ordered by their priority.
  We learned in time that sending the mail only on the first MX is not a good practice.
  When delivering large amounts of emails we needed to balance the amount of connections to a single provider.
  We also experienced bad recipient MX records and thus never take a rejection as final until you attempt once more to another MX.

4. [NotifyingThread](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/Threads/NotifyingThread.java) and [ThreadCompleteListener](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/Threads/ThreadCompleteListener.java) Classes
  A nice method to keep the count of running threads specialized for a kind of job.
  This is helpful if you want to have a given number of workers which will process a queue.

5. [IpQueue Class](https://github.com/tntu/xmail-java/blob/master/src/com/xmail/XmailService/IpQueue.java)
  A singleton static class used to implement a pool of outgoing (binding) IP addresses.
  This is helpful if you have many IP addresses bound to your server and you want to use them balanced (Round Robin).
  
# IntelliJ - Create Project from this sources
  1. Clone this project to your computer and add it as IntelliJ project
  
  2. Scroll in the left Project pane for **tests** folder.
  Right click on it, select **Mark directory as** and choose **Sources root**
  
  3. Go to **Run** -> **Edit Configurations**
  
  4. Click on green "+" and choose **Application**
  
  5. Name it as **Main**, and add ```com.xmail.Main``` as **Main Class**
  
  6. Click again on green "+" sign and choose again **Application**
  
  7. Name it as **Test**, and add ```com.tests.Main``` as **Main Class**
  
# IntelliJ - Before to Run
You must add a Maven Goal for com.xmail.Main and for com.tests.Main as follow:
  1. **Run** -> **Edit Configurations**
  
  2. Select **Main** from **Applications** on the left
  
  3. In **Before Launch** area click on "+" and select **Run Maven Goal**
  
  4. Add the next code to **Command line** field: ```org.javalite:activejdbc-instrumentation:1.4.10:instrument```
  
  5. Do the same thing for **Test** (com.test.Main)

For a better understanding, please:
  * watch this video: [ActiveJDBC + IntelliJ Idea + Instrumentation](https://www.youtube.com/watch?v=OHXJXzZNKCU) or 
  * read the documentation: [Javalite.io instrumentation for IntelliJ](http://javalite.io/instrumentation#video-intellij-idea-instrumentation).

# How to test
1. In order to test **xmail-java**, you need to do few edits in [XmailConfig]():
  1. Change the path where the SQLite database will be saved on disk ```public static String dbPath = "/path/to/xmail-java/data/data.db";```. You should provide a read/write access to that file.
  2. Change the path where the emails will be stored on disk ```public static String mailPath = "/path/to/xmail-java/mail/";```. You should provide a read/write access to that file.
  3. Change the remote SMTP port ```public static int port = 25;```. Some ISPs blocks the default 25 port. Some MTA provide an alternative port for SMTP, like port 26. You can also try the nonstandard submission 587 port (if the MTA doesn't require authenitcation on it).
  4. Add the bound IP addresses to your machine (if you support this):
  ```
    public static String[] outgoingIPv4 = new String[] {
      "0.0.0.0",
    };
    public static String[] outgoingIPv6 = new String[] {
      "::1",
    };
    public static boolean ipv6Enabled = false;
  ```
2. Edit the [ComposerTest](https://github.com/tntu/xmail-java/blob/master/tests/com/tests/ComposerTest.java) file.
  ```
  String from = "from@example.com";

  String to = "to@example.com";
  String subject = "Cool Test message";
  String message = "<p>Hi there!!</p><p>How are you?</p><p><br/></p><p>I hope to receive well this message</p>";
  String headers = "From: <from@example.com>";
  String[] attachments = new String[] {};
  ```
3. Run The com.tests/Main.java for few times and after you can start the service com.xmail/Main.java. You can also run the tests during the service process is running.

# Other requirements
This library requires [Apache log4j](http://logging.apache.org/log4j/1.2/) and [ActiveJDBC](http://javalite.io/activejdbc).