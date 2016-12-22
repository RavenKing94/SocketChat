package com.kasra_shamsaei;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class Main {

    private Scanner scanner = new Scanner(System.in);
    private String input;
    private int port;
    private String ip;
    private SocketChannel socket;
    private ServerSocketChannel serverSocket;
    private ByteBuffer bb = ByteBuffer.allocate(512);

    public static void main(String[] args) {
        new Main();
    }

    public Main(){
        // Connection Loop
        while (true){
            input = Read("Enter Mode (1-Server, 2-Client) : ");
            int mode = getMode(input);

            if (mode == -1){
                Write("<ERR> : wrong mode !");
                continue;
            }
            if (mode == 1){
                port = Integer.valueOf(Read("Enter Port number : "));
                runServer(port);
            }
            if (mode == 2){
                ip = Read("Enter IP Address : ");
                port = Integer.valueOf(Read("Enter Port number : "));
                runClient(ip,port);
            }
        }
    }

    private void runServer(int port){
        try {
            serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Connection Loop
        while (true){
            WriteLn("Listening ...");
            while (serverSocket.validOps()!= SelectionKey.OP_ACCEPT);
            try {
                socket = serverSocket.accept();
                WriteLn("<OK> Client Connected !");
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            connectionLoop(socket);
        }

    }

    private void runClient(String ip, int port){
        SocketChannel sck;
        try {
            sck = SocketChannel.open(new InetSocketAddress(ip,port));
        } catch (IOException e) {
            WriteLn("<ERR> Cannot connect to server "+ip+":"+port);
            return;
        }

        while (true){
            connectionLoop(sck);
        }
    }

    private void connectionLoop(SocketChannel socket){
        Thread rd = new Thread(new Reader(socket));
        rd.start();
        while (true){
            String msg = Read("");
            while ((socket.validOps() & OP_WRITE) == 0){
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
            try {
                socket.write(ByteBuffer.wrap((msg + "\r\n").getBytes()));
                WriteLn(">> Sent ("+(msg.length()+2)+")bytes : { "+msg+"[CR][LF] }");
            } catch (IOException e) {
                //e.printStackTrace();
                WriteLn("<ERR> Client Disconnected !");
                break;
            }

            if (rd.getState() == Thread.State.TERMINATED){
                rd.interrupt();
                break;
            }
        }
    }

    class Reader implements Runnable{
        SocketChannel socket;
        Reader(SocketChannel socket){
            this.socket = socket;
        }
        @Override
        public void run() {
            while (true) {
                if ((socket.validOps() & OP_READ)!=0) {
                    try {
                        int len = socket.read(bb);
                        if (len != -1) {
                            Write(">> Received String : { ");
                            //bb.flip();
                            String str = new String(bb.array(),0,len-2);
                            Write(str);
                            WriteLn("[CR][LF] }");
                            Write(">> Received Bytes("+len+") : [ ");
                            for (int i = 0; i < str.length(); i++) {
                                Write((str.getBytes()[i])+" ");
                            }
                            WriteLn("]");
                            if (str.equals("@EXIT")){
                                WriteLn("Disconnecting ...");
                                socket.close();
                                return;
                            }
                            bb.clear();
                        }
                        else throw new IOException("read len = -1");
                    } catch (IOException e) {
                        //e.printStackTrace();
                        WriteLn("<ERR> Client Disconnected !");
                        return;
                    }
                }
            }
        }
    }

    private String Read(String msg){
        if (!msg.equals(""))
            System.out.println(msg);
        return scanner.nextLine();
    }

    private void WriteLn(String msg){
        System.out.println(msg);
    }

    private void Write(String msg){
        System.out.print(msg);
    }

    private int getMode(String mode){
        if (mode==null){
            return -1;
        }else if (mode.equals("1")){
            return 1;
        }else if (mode.equals("2")){
            return 2;
        }else {
            return -1;
        }
    }
}
