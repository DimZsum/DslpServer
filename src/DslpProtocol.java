import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class DslpProtocol {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

    private enum State {
        WAITING,
        RECEIVINGTYPE,
        RECEIVING,
        ENDING,
    }

    private static final String START_LINE = "dslp/1.2";
    private static final String END_LINE = "dslp/end";

    private static final int ERROR = -1;
    private static final int REQUEST_TIME = 0;
    private static final int GROUP_JOIN = 1;
    private static final int GROUP_LEAVE = 2;
    private static final int GROUP_NOTIFY = 3;
    private static final int PEER_NOTIFY = 4;

    private int receivingType;
    private State state;
    private String currentGroup;
    private String currentMessage;
    private boolean skipLineFlag;
    private boolean endLoop;

    private LinkedList<String> groupList;
    private String currentIP;

    DslpProtocol(LinkedList<String> groupList) {
        this.groupList = groupList;
        this.currentIP = "";
        this.receivingType = ERROR;
        this.state = State.WAITING;
        this.currentGroup ="";
        this.currentMessage = "";
        this.skipLineFlag = false;
        this.endLoop = false;
    }

    public String processInput(String input) {

        if (state == State.ENDING) {
            if (input.equals(END_LINE)) {
                return processEndMessage(receivingType);
            } else return processErrorMessage(input);
        }

        if (state == State.RECEIVING) {
            processReceivingMessage(receivingType, input, groupList);
            if(endLoop) {
                state = State.WAITING;
                return processEndMessage(receivingType);
            }
        }

        if (state == State.RECEIVINGTYPE) {
            setReceivingType(input);
            if (receivingType == ERROR) return processErrorMessage(input);
        }

        if (state == State.WAITING) {
            if (input.equals(START_LINE)){
                state = State.RECEIVINGTYPE;
                skipLineFlag = false;
                endLoop = false;
                currentGroup ="";
                currentMessage = "";
                currentIP = "";
            }
            else return processErrorMessage(input);
        }

        return null;
    }

    private String processEndMessage(int receivingType) {
        state = State.WAITING;
        switch (receivingType) {
            case REQUEST_TIME:
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                return "dslp/1.2\r\nresponse time\r\n" + sdf.format(timestamp) + "\r\ndslp/end";
            case GROUP_JOIN:
                return null;
            case GROUP_NOTIFY:
                return "//GROUPNOTIFYFLAG//\r\ndslp/1.2\r\n" +
                        "group notify\r\n"+
                        currentGroup + "\r\n" +
                        currentMessage +
                        "dslp/end";
            case PEER_NOTIFY:
                return "//PEERNOTIFYFLAG//\r\ndslp/1.2\r\n" +
                        "peer notify\r\n"+
                        currentIP + "\r\n" +
                        currentMessage +
                        "dslp/end";

        }
        return null;
    }

    private void processReceivingMessage(int receivingType, String input, LinkedList<String> groupList) {
        switch (receivingType) {
            case GROUP_JOIN:
                state = State.ENDING;
                groupList.add(input);
                break;
            case GROUP_LEAVE:
                state = State.ENDING;
                groupList.remove(input);
                break;
            case GROUP_NOTIFY:
                if(input.equals(END_LINE)){
                    endLoop = true;
                    break;
                }
                if(skipLineFlag){
                    this.currentGroup = input;
                    skipLineFlag = false;
                    break;
                } else {
                    this.currentMessage = this.currentMessage +input + "\r\n";
                    break;
                }
            case PEER_NOTIFY:
                if(input.equals(END_LINE)){
                    endLoop = true;
                    break;
                }
                if(skipLineFlag){
                    this.currentIP = input;
                    skipLineFlag = false;
                    break;
                } else {
                    this.currentMessage = this.currentMessage +input + "\r\n";
                    break;
                }
        }
    }

    private void setReceivingType(String input) {
        if (input.equals("request time")) {
            receivingType = REQUEST_TIME;
            state = State.ENDING;
        }
        if (input.equals("group join")){
            receivingType = GROUP_JOIN;
            state = State.RECEIVING;
        }
        if (input.equals("group leave")){
            receivingType = GROUP_LEAVE;
            state = State.RECEIVING;
        }
        if (input.equals("group notify")){
            receivingType = GROUP_NOTIFY;
            state = State.RECEIVING;
            skipLineFlag = true;
        }
        if (input.equals("peer notify")){
            receivingType = PEER_NOTIFY;
            state = State.RECEIVING;
            skipLineFlag = true;
        }

        if (input.equals("error")) receivingType = ERROR;
    }

    private String processErrorMessage(String input){
        return "dslp/1.2\r\n" +
                "error\r\n"+
                "dont know what '" +input + "' means\r\n" +
                "Please consult the DSLP 1.2 specification for a list of valid message types.\r\n" +
                "dslp/end";
    }

    public String getCurrentGroup(){
        return this.currentGroup;
    }
    public String getCurrentIP(){
        return this.currentIP;
    }
}

