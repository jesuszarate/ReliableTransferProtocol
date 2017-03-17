/**
 * Written by
 *
 * Jesus Zarate
 */
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
    private Packet bPacket;

    private boolean inTransit;

    private int TIME = 130;

    /* ********Stats********* */
    private int transmittedNum = 0;
    private int retransmissionsNum = 0;
    private int ACKNum = 0;
    private int corruptedPacketNum = 0;
    private double totalRTT = 0;
    private double startTime = 0;
    private int RTT = 0;
    /* ********Stats********* */

    /**
     * Add any necessary class variables here.  Remember, you cannot use
     * these variables to send messages error free!  They can only hold
     * state information for A or B.
     * Also add any necessary methods (e.g. checksum of a String)
     * <p>
     * This is the constructor.  Don't touch!
     */
    public StudentNetworkSimulator(int numMessages, double loss, double corrupt, double avgDelay, int trace, long seed) {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
    }

    public void runSimulator(){
        super.runSimulator();

        printStats();
    }

    /**
     * This routine will be called whenever the upper layer at the sender [A]
     * has a message to send.  It is the job of your protocol to insure that
     * the data in such a message is delivered in-order, and correctly, to
     * the receiving upper layer.
     *
     * @param message
     */
    protected void aOutput(Message message) {
        if (!inTransit){
            aPacket = makePacket(aSeqNum, A, message.getData());
            send(aPacket);
            inTransit = true;
            startTime = getTime();
            transmittedNum++;
        }else {
            System.out.println("A: message - " + message.getData() + "was dropped");
        }
    }



    /**
     * This routine will be called whenever a packet sent from the B-side
     * (i.e. as a result of a toLayer3() being done by a B-side procedure)
     * arrives at the A-side.  "packet" is the (possibly corrupted) packet
     * sent from the B-side.
     *
     * @param packet
     */
    protected void aInput(Packet packet) {
        updateRTT();

        System.out.println("A: rcv ACK" + packet.getAcknum());
        Packet newPacket = makePacketForChecksum(packet);

        // Check to make sure that the Acknowledgement is for the correct packet
        if (!corrupt(newPacket) && packet.getAcknum() == aSeqNum) {
            System.out.println("Packet" + packet.getAcknum() + " was acknowledged");
            System.out.println("Stopped timer");
            aSeqNum = computeSeqNum(aSeqNum);
            inTransit = false;
            stopTimer(A);

        } else if (corrupt(newPacket)) {
            System.out.println("Corrupted ACK");
            corruptedPacketNum++;
        } else {
            System.out.println("Wrong ACK");
        }
    }

    private void updateRTT() {
        totalRTT += getTime() - startTime;
        RTT++;
    }

    /**
     * This routine will be called when A's timer expires (thus generating a
     * timer interrupt). You'll probably want to use this routine to control
     * the retransmission of packets. See startTimer() and stopTimer(), above,
     * for how the timer is started and stopped.
     */
    protected void aTimerInterrupt() {
        System.out.println("Timed out........" + aPacket.getPayload());
        send(aPacket);
        startTime = getTime();
        retransmissionsNum++;
    }

    /**
     * This routine will be called once, before any of your other A-side
     * routines are called. It can be used to do any required
     * initialization (e.g. of member variables you add to control the state
     * of entity A).
     */
    protected void aInit() {
        aSeqNum = 0;
        inTransit = false;
    }

    /**
     * This routine will be called whenever a packet sent from the B-side
     * (i.e. as a result of a toLayer3() being done by an A-side procedure)
     * arrives at the B-side.  "packet" is the (possibly corrupted) packet
     * sent from the A-side.
     *
     * @param packet
     */
    protected void bInput(Packet packet) {
        System.out.println("B: rcv pkt" + packet.getSeqnum() + " data: " + packet.getPayload());
        if (!corrupt(packet) && packet.getSeqnum() == bSeqNum) {

            bPacket = packet;

            toLayer5(B, packet.getPayload());

            ACKNum++;

            bSeqNum = computeSeqNum(bSeqNum);
        } else if (corrupt(packet) || packet.getSeqnum() != bSeqNum) {

            if (corrupt(packet)) {
                System.out.println("Detected a corrupt packet from A");
                corruptedPacketNum++;
            }

            if (packet.getSeqnum() != bSeqNum) {
                System.out.println("Detected duplicate A: " + packet.getSeqnum() + " instead of " + bSeqNum);
            }
        }
        sendACK(bPacket);
        transmittedNum++;
    }

    /**
     * This routine will be called once, before any of your other B-side
     * routines are called. It can be used to do any required
     * initialization (e.g. of member variables you add to control the state
     * of entity B).
     */
    protected void bInit() {
        bSeqNum = 0;
    }


    /* *******Helper Methods***************/

    /**
     * Send the given packet from A to B
     *
     * @param packet Packet to be sent over
     */
    private void send(Packet packet) {
        toLayer3(A, packet);

        startTimer(A, 130);

        String data = " data: " + packet.getPayload();
        System.out.println("A: sent pkt" + packet.getSeqnum() + data);
        System.out.println("started timer\n");
    }

    /**
     * Send an acknowledgement for the given packet
     *
     * @param packet packet to acknowledge
     */
    private void sendACK(Packet packet) {
        System.out.println("send ACK" + packet.getSeqnum() + "\n");
        int checksum = computeChecksum(bSeqNum, packet.getSeqnum(), packet.getPayload());
        toLayer3(B, new Packet(bSeqNum, packet.getSeqnum(), checksum, "Dummy Payload"));
    }

    /**
     * Checks to make sure the packet has not been corrupted
     *
     * @param packet Packet to check if it's corrupt
     * @return true if the packet is not corrupt
     */
    private boolean corrupt(Packet packet) {
        int checksum = computeChecksum(packet.getSeqnum(), packet.getAcknum(), packet.getPayload());
        return checksum != packet.getChecksum();
    }

    /**
     * Makes a packet with the given parameters
     *
     * @param seqNum  sequence number
     * @param ack     ACK number
     * @param payload Payload
     * @return New packet
     */
    private Packet makePacket(int seqNum, int ack, String payload) {
        int checkSum = computeChecksum(seqNum, ack, payload);
        return new Packet(seqNum, ack, checkSum, payload);
    }

    private Packet makePacketForChecksum(Packet packet) {
        Packet newPacket = new Packet(packet);
        newPacket.setPayload(aPacket.getPayload());
        newPacket.setChecksum(packet.getChecksum());
        return  newPacket;
    }

    /**
     * Computes the checksum based on the given parameters
     *
     * @param seqNum  sequnce number
     * @param ack     ACK number
     * @param payload Payload
     * @return The Checksum
     */
    private int computeChecksum(int seqNum, int ack, String payload) {
        int charSum = 0;
        for (int i = 0; i < payload.length(); i++) {
            charSum += payload.charAt(i);
        }
        return ack + seqNum + charSum;
    }

    /**
     * Alternates the sequence number
     *
     * @param seqNum Current sequence number
     * @return Next sequence number
     */
    private int computeSeqNum(int seqNum) {
        return seqNum == 1 ? 0 : 1;
    }


    private void printStats() {
        //System.out.println("nCorrupt " + nLost);
        System.out.println("transmitted: " + transmittedNum);
        System.out.println("retransmissions: " + retransmissionsNum);
        System.out.println("ACKs: " + ACKNum);
        System.out.println("corrupted Packets: " + corruptedPacketNum);
        System.out.println("avgRTT: " + (RTT != 0 ? totalRTT/RTT : 0) + "\n");
    }
}
