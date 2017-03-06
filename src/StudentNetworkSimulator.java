public class StudentNetworkSimulator extends NetworkSimulator {
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B
     *
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(int entity, String dataSent)
     *       Passes "dataSent" up to layer 5 from "entity" [A or B]
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          create a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    private int aSeqNum;
    private Packet aPacket;

    private int bSeqNum;

    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages, double loss, double corrupt, double avgDelay, int trace, long seed) {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message) {

        aSeqNum = computeSeqNum(aSeqNum);

        aPacket = makePacket(aSeqNum, 0, message.getData());

        send(aPacket);
    }

    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet) {
        // If the checksum is good to go then that means it's not corrupt

        System.out.println("rcv ACK" + packet.getAcknum());
        Packet newPacket = new Packet(packet);
        newPacket.setPayload(aPacket.getPayload());
        newPacket.setChecksum(packet.getChecksum());

        if (notCorrupt(newPacket) && packet.getAcknum() == aSeqNum) {
            // Do nothing yet here but stop timer later on
            System.out.println("Acknowledged packet" + packet.getAcknum());
            stopTimer(0);
        }
    }

    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt() {
        System.out.println("Timed out........" + aPacket.getPayload());
        send(aPacket);
    }

    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit() {
        aSeqNum = 1;
    }

    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet) {
        System.out.println("rcv pkt" + packet.getSeqnum());
        if (notCorrupt(packet) && packet.getSeqnum() == bSeqNum) {

            toLayer5(1, packet.getPayload());

            System.out.println("send ACK" + packet.getSeqnum() + "\n");
            int checksum = computeChecksum(bSeqNum, packet.getSeqnum(), packet.getPayload());
            toLayer3(1, new Packet(bSeqNum, packet.getSeqnum(), checksum));
            bSeqNum = computeSeqNum(bSeqNum);
        }
    }

    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit() {
        bSeqNum = 0;
    }

    /********Helper Methods***************/
    private void send(Packet packet){
        toLayer3(0, packet);

        startTimer(0, 10);

        String data = " data: " + packet.getPayload();
        System.out.println("sent pkt" + aSeqNum + data);
        System.out.println("start timer\n");
    }

    private boolean notCorrupt(Packet packet) {
        int checksum = computeChecksum(packet.getSeqnum(), packet.getAcknum(), packet.getPayload());
        return checksum == packet.getChecksum();
    }

    private Packet makePacket(int seqNum, int ack, String payload) {
        int checkSum = computeChecksum(seqNum, ack, payload);
        return new Packet(seqNum, ack, checkSum, payload);
    }

    private int computeChecksum(int seqNum, int ack, String payload) {
        int charSum = 0;
        for (int i = 0; i < payload.length(); i++) {
            charSum += payload.charAt(i);
        }
        return ack + seqNum + charSum;
    }

    private int computeSeqNum(int seqNum) {
        return seqNum == 1 ? 0 : 1;
    }
}
