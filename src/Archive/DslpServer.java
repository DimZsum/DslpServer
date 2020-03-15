import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;


public class DslpServer {

    private static class DslpServerThread extends Thread {
        private Socket clientSocket;
        private LinkedList<DslpServerThread> clientList;
        private LinkedList<String> groupList;
        private static int CUT_GROUP_NOTIFY_FLAG = 21;
        private static int CUT_PEER_NOTIFY_FLAG = 20;

        private DslpServerThread(Socket clientSocket, LinkedList<DslpServerThread> clientList) {
            this.clientSocket = clientSocket;
            groupList = new LinkedList<String>();

            this.clientList = clientList;
        }

        public LinkedList<String> getGroupList(){
            return groupList;
        }

        public Socket getClientSocket() {
            return clientSocket;
        }

        @Override
        public void run() {
            try {
                String inputLine, outputLine;
                PrintWriter out = new PrintWriter(this.clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                DslpProtocol dslp = new DslpProtocol(groupList);
                while ((inputLine = in.readLine()) != null) {
                    outputLine = dslp.processInput(inputLine);
                    if(outputLine != null){
                        if(outputLine.contains("//GROUPNOTIFYFLAG//")){
                            outputLine = outputLine.substring(CUT_GROUP_NOTIFY_FLAG);
                            for(DslpServerThread client : clientList) {
                                for(String group : client.getGroupList()){
                                    if (group.equals(dslp.getCurrentGroup()) && !client.equals(this)){
                                        PrintWriter outGroup = new PrintWriter(client.getClientSocket().getOutputStream(), true);
                                        outGroup.println(outputLine);
                                    }
                                }
                            }
                        }
                        if(outputLine.contains("//PEERNOTIFYFLAG//")){
                            outputLine = outputLine.substring(CUT_PEER_NOTIFY_FLAG);
                            for(DslpServerThread client : clientList) {
                                if(client.getClientSocket().getInetAddress().toString().equals("/" + dslp.getCurrentIP()) &&  !client.equals(this)){
                                    PrintWriter outGroup = new PrintWriter(client.getClientSocket().getOutputStream(), true);
                                    outGroup.println(outputLine);
                                }
                            }
                        }
                        out.println(outputLine);
                    }
                }
            } catch (Exception e) {

            }
        }
    }

    public static void main(String[] args) {
        int portNumber = 44444;
        try {
            LinkedList<DslpServerThread> clientList = new LinkedList<DslpServerThread>();
            ServerSocket serverSocket = new ServerSocket(portNumber);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                DslpServerThread newClient = new DslpServerThread(clientSocket, clientList);
                clientList.add(newClient);
                newClient.start();

            }
        } catch (Exception e) {

        }
    }
}
