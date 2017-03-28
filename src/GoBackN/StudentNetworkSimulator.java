package GoBackN;

import java.util.ArrayList;

/**
 * Written by
 * <p>
 * Jesus Zarate
 */
public class StudentNetworkSimulator extends NetworkSimulator {
    private static final String DUMMY_PAYLOAD = "Dummy Payload";
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the AlternatingBit.Message data and
     *                     AlternatingBit.Packet payload
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
     *  void toLayer3(int callingEntity, AlternatingBit.Packet p)
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
     *  AlternatingBit.Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      AlternatingBit.Message(String inputData):
     *          creates a new AlternatingBit.Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing AlternatingBit.Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  AlternatingBit.Packet: Used to encapsulate a packet
     *    Constructors:
     *      AlternatingBit.Packet (AlternatingBit.Packet p):
     *          creates a new AlternatingBit.Packet that is a copy of "p"
     *      AlternatingBit.Packet (int seq, int ack, int check, String newPayload)
     *          creates a new AlternatingBit.Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      AlternatingBit.Packet (int seq, int ack, int check)
     *          create a new AlternatingBit.Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the AlternatingBit.Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the AlternatingBit.Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the AlternatingBit.Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the AlternatingBit.Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the AlternatingBit.Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the AlternatingBit.Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the AlternatingBit.Packet
     *      int getPayload()
     *          returns the AlternatingBit.Packet's payload
     *
     */

    private Packet bPacket;

    private int TIME = 60;

    /* ********Stats********* */
    private int transmittedNum = 0;
    private int retransmissionsNum = 0;
    private int ACKNum = 0;
    private int packetLossNum = 0;
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
    @SuppressWarnings("WeakerAccess")
    public StudentNetworkSimulator(int numMessages, double loss, double corrupt, double avgDelay, int trace, long seed) {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
    }

    public void runSimulator() {
        super.runSimulator();

        printStats();
    }

    private ArrayList<Packet> sndpkt;
    private int nextSeqNum;
    private int base;
    private int N;
    private int MaxBuffer;

    private int expectedSeqNum;

    /**
     * This routine will be called whenever the upper layer at the sender [A]
     * has a message to send.  It is the job of your protocol to insure that
     * the data in such a message is delivered in-order, and correctly, to
     * the receiving upper layer.
     *
     * @param message Message
     */
    protected void aOutput(Message message) {

        if (nextSeqNum < getBufferSize()) {
            sndpkt.add(makePacket(nextSeqNum, A, message.getData()));
            send();
            transmittedNum++;
        } else {
            refuse_data(message.getData());
        }
    }

    /**
     * This routine will be called whenever a packet sent from the B-side
     * (i.e. as a result of a toLayer3() being done by a B-side procedure)
     * arrives at the A-side.  "packet" is the (possibly corrupted) packet
     * sent from the B-side.
     *
     * @param packet Packet
     */
    protected void aInput(Packet packet) {
        updateRTT();

        System.out.println("A: rcv ACK" + packet.getAcknum());
        Packet newPacket = makePacketForChecksum(packet);

        // Check to make sure that the Acknowledgement is for the correct packet
        if (!isCorrupt(newPacket)) {
            base = packet.getAcknum() + 1;

            if (base == nextSeqNum) {
                System.out.println("A: Packet" + packet.getAcknum() + " was acknowledged");
                System.out.println("A: Stopped timer");
                stopTimer(A);
            }

        } else if (isCorrupt(newPacket)) {
            System.out.println("A: received a corrupted ACK");
            corruptedPacketNum++;
        } else {
            System.out.println("A: received wrong ACK");
        }
    }

    /**
     * This routine will be called when A's timer expires (thus generating a
     * timer interrupt). You'll probably want to use this routine to control
     * the retransmission of packets. See startTimer() and stopTimer(), above,
     * for how the timer is started and stopped.
     */
    protected void aTimerInterrupt() {
        startTimer(A, TIME);

        packetLossNum++;
        System.out.println("A: Timed out........");
        startTime = getTime();

        for (int pos = base; pos < nextSeqNum; pos++) {
            System.out.println("A: Resending packet" + pos);
            send(sndpkt.get(pos));
            retransmissionsNum++;
        }
    }

    /**
     * This routine will be called once, before any of your other A-side
     * routines are called. It can be used to do any required
     * initialization (e.g. of member variables you add to control the state
     * of entity A).
     */
    protected void aInit() {
        nextSeqNum = 0;
        base = 0;
        N = 8; // Window size
        sndpkt = new ArrayList<>();
        MaxBuffer = 50;
    }

    /**
     * This routine will be called whenever a packet sent from the B-side
     * (i.e. as a result of a toLayer3() being done by an A-side procedure)
     * arrives at the B-side.  "packet" is the (possibly corrupted) packet
     * sent from the A-side.
     *
     * @param packet Packet
     */
    protected void bInput(Packet packet) {
        System.out.println("B: rcv pkt" + packet.getSeqnum() + " data: " + packet.getPayload());
        if (!isCorrupt(packet) && packet.getSeqnum() == expectedSeqNum) {

            bPacket = packet;
            toLayer5(B, packet.getPayload()); // Extract and deliver the data
            System.out.println("B: deliver pkt " + packet.getSeqnum());

            ACKNum++;

            sendACK(makePacket(bPacket.getSeqnum(), bPacket.getAcknum(), DUMMY_PAYLOAD));
            expectedSeqNum++;

        } else if (isCorrupt(packet) || packet.getSeqnum() != expectedSeqNum) {

            if (isCorrupt(packet)) {
                System.out.println("B: Detected a corrupt packet from A");
                corruptedPacketNum++;
            }

            if (packet.getSeqnum() != expectedSeqNum) {
                System.out.println("B: Detected out of order packet from A: " + packet.getSeqnum() + " instead of " + expectedSeqNum);
                System.out.println("B: discard pkt " + packet.getSeqnum());
            }
            sendACK(bPacket);
        }
        transmittedNum++;
    }

    /**
     * This routine will be called once, before any of your other B-side
     * routines are called. It can be used to do any required
     * initialization (e.g. of member variables you add to control the state
     * of entity B).
     */
    protected void bInit() {
        expectedSeqNum = 0;
        bPacket = new Packet(0, 0, 0, DUMMY_PAYLOAD);
    }


    /* *******Helper Methods***************/

    /**
     * Send packets that are in the window
     */
    private void send() {

        while (nextSeqNum < base + N) {

            if (nextSeqNum >= sndpkt.size()) {
                System.out.println("A: Buffer empty");
                break;
            }

            send(sndpkt.get(nextSeqNum));

            if (base == nextSeqNum) {
                System.out.println("A: Timer started with next sequence number of: " + nextSeqNum);
                startTimer(A, TIME);
            }
            nextSeqNum++;
        }
    }

    /**
     * Send the given packet from A to B
     *
     * @param packet Packet to be sent over
     */
    private void send(Packet packet) {
        toLayer3(A, packet);

        String data = " data: " + packet.getPayload();
        System.out.println("A: sent pkt" + packet.getSeqnum() + data);
        System.out.println("A: started timer\n");
    }

    /**
     * For B: Send an acknowledgement for the given packet
     *
     * @param packet packet to acknowledge
     */
    private void sendACK(Packet packet) {
        System.out.println("B: send ACK" + packet.getSeqnum() + "\n");
        //params passed in             (Expected sequence number, ACK,                payload
        int checksum = computeChecksum(expectedSeqNum,            packet.getSeqnum(), packet.getPayload());
        toLayer3(B, new Packet(expectedSeqNum, packet.getSeqnum(), checksum, DUMMY_PAYLOAD));
    }

    /**
     * Checks to make sure the packet has not been corrupted
     *
     * @param packet Packet to check if it's isCorrupt
     * @return true if the packet is not isCorrupt
     */
    private boolean isCorrupt(Packet packet) {
        int checksum = computeChecksum(packet.getSeqnum(), packet.getAcknum(), packet.getPayload());
        return checksum != packet.getChecksum();
    }

    private int getBufferSize() {
        return base + N + MaxBuffer;
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

    /**
     * This packet is specifically just for the checksum
     * Adds the current packet data of the packet to a new packet along
     * with the checksum
     *
     * @param packet AlternatingBit.Packet returned from user
     * @return new packet with the same payload as the passed in packet
     */
    private Packet makePacketForChecksum(Packet packet) {
        Packet newPacket = new Packet(packet);
        newPacket.setPayload(packet.getPayload());
        newPacket.setChecksum(packet.getChecksum());
        return newPacket;
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
     * Update the Return Trip Time
     */
    private void updateRTT() {
        totalRTT += getTime() - startTime;
        RTT++;
    }

    private void refuse_data(String data) {
        System.out.println("A: message - " + data + "was dropped");
    }

    private void printStats() {
        System.out.println("\nSTATS");
        //System.out.println("nCorrupt " + nLost);
        System.out.println("Transmitted: " + transmittedNum);
        System.out.println("Retransmissions: " + retransmissionsNum);
        System.out.println("ACKs: " + ACKNum);
        System.out.println("Packets lost: " + packetLossNum);
        System.out.println("Corrupted Packets: " + corruptedPacketNum);
        System.out.println("AvgRTT: " + (RTT != 0 ? totalRTT / RTT : 0) + "\n");
    }
}
