package dev.aincrad;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Isolated TCP receiver. It never relays mail to the Internet. */
final class SmtpInbox implements AutoCloseable {
    record Message(String from,String to,String subject,String text){}
    final ServerSocket server;final BlockingQueue<Message> inbox=new LinkedBlockingQueue<>();final Thread worker;
    volatile boolean closing;
    SmtpInbox()throws IOException{server=new ServerSocket(0,10,InetAddress.getLoopbackAddress());worker=new Thread(this::run,"test-smtp");worker.setDaemon(true);worker.start();}
    int port(){return server.getLocalPort();}
    void run(){while(!closing)try(Socket socket=server.accept()){
        socket.setSoTimeout(5000);var in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));var out=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8),true);
        out.print("220 local-test ESMTP\r\n");out.flush();String line;
        while((line=in.readLine())!=null){
            if(line.startsWith("EHLO")||line.startsWith("HELO"))out.print("250 local-test\r\n");
            else if(line.equals("DATA")){out.print("354 End with dot\r\n");out.flush();StringBuilder body=new StringBuilder();while((line=in.readLine())!=null&&!line.equals("."))body.append(line.startsWith("..")?line.substring(1):line).append("\r\n");
                var message=new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(body.toString().getBytes(StandardCharsets.UTF_8)));
                inbox.add(new Message(((jakarta.mail.internet.InternetAddress)message.getFrom()[0]).getAddress(),((jakarta.mail.internet.InternetAddress)message.getAllRecipients()[0]).getAddress(),message.getSubject(),String.valueOf(message.getContent())));out.print("250 queued-locally\r\n");
            }else if(line.equals("QUIT")){out.print("221 bye\r\n");out.flush();break;}
            else out.print("250 ok\r\n");out.flush();
        }
    }catch(Exception error){if(!closing)throw new RuntimeException(error);}}
    Message await(String subject)throws InterruptedException{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);while(System.nanoTime()<end){Message m=inbox.poll(100,TimeUnit.MILLISECONDS);if(m!=null&&m.subject().contains(subject))return m;}throw new AssertionError("No llegó el correo local esperado");}
    public void close()throws IOException{closing=true;server.close();}
}
