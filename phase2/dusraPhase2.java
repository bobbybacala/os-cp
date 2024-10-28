// according to the structure giving by Aparna Maam

import java.io.*;
import java.util.*;

// structure of the PCB program control board
class PCB {
    // the job description variables
    // job id, total time limit, total line limit
    int JID;
    int TTL;
    int TLL;

    // constructor to initialise the values of the job desc variables
    PCB(int jid, int ttl, int tll) {
        this.JID = jid;
        this.TTL = ttl;
        this.TLL = tll;
    }
}

public class dusraPhase2 {

    // Hardware resources variables
    private char memory[][] = new char[300][4];

    // Private Access to OS Function
    private char instructionRegister[] = new char[4];
    private int instructionCounter;
    private char aRegister[] = new char[4];
    private boolean toggleRegister;

    // interrupts
    private int SI;
    private int PI;
    private int TI;

    // pointer register
    private int PTR;

    // to count instruction read on the prg card
    private int count;

    // real address
    private int realAddress;

    // helper counters to keep time of total time counter and total line coutner
    private int TTC;
    private int TLC;

    private int ptrPointer;

    // a datastructure to keep a track whether the num is generated or not
    private ArrayList<Integer> allocateList = new ArrayList<>();

    // input output files
    private String inputFile;
    private String outputFile;

    // objects of buffer reader and writers
    private BufferedReader input;
    private BufferedWriter output;

    // a map to keep track of virtual address and real address
    private HashMap<Integer, Integer> theMap = new HashMap<>();

    // initialise the PCB with everything as 0
    PCB processBoard = new PCB(0, 0, 0);

    dusraPhase2(String inputfile, String outputfile) throws Exception {
        // initialise the input fileand output file
        this.inputFile = inputfile;
        this.outputFile = outputfile;

        // create object for the files
        File fileR = new File(inputFile);
        File fileW = new File(outputFile);

        // create buffered reader writer output
        input = new BufferedReader(new FileReader(fileR));
        output = new BufferedWriter(new FileWriter(fileW));
    }

    private boolean cardReader[] = new boolean[2];
    // 0 = Control card || 1 = Data card

    // Private Functions

    // chalo chalo loading karo
    private void load() throws Exception {
        System.out.println("Enter in load Function");
        String Reader = input.readLine();
        System.out.println(Reader);

        // keep reading until end of file
        while (Reader != null) {

            // loading Logic
            // loading Control Card Data
            if (Reader.contains("$AMJ")) {
                // PID,TTL,TLL
                int temp[] = new int[3];
                int j = 0;
                for (int i = 4; i < Reader.length(); i += 4) {
                    temp[j] = Integer.parseInt(Reader.substring(i, i + 4));
                    j++;
                }

                processBoard = new PCB(temp[0], temp[1], temp[2]);
                cardReader[0] = true;
            } else if (Reader.contains("$DTA")) {
                printPageTable(PTR);
                printPCB(processBoard);
                startExecution();
                cardReader[1] = true;
            } else if (Reader.contains("$END")) {
                printMemory();
                init();

            } else if (!Reader.contains("$") && cardReader[0] && !cardReader[1]) {

                int loc = allocate();
                memory[ptrPointer][0] = '1';
                memory[ptrPointer][2] = (char) ((loc / 10) + '0');
                memory[ptrPointer][3] = (char) ((loc % 10) + '0');
                ptrPointer++;

                int row = loc * 10;
                int col = 0;

                if (Reader.length() > 40) {
                    Reader = Reader.substring(0, 40);
                }
                for (char i : Reader.toCharArray()) {
                    if (row < 300) {
                        memory[row][col % 4] = i;
                        col++;
                    } else {
                        System.out.println("Memory Limit Exceed!!");
                    }
                    if (col % 4 == 0) {
                        row++;
                    }
                }
            }

            Reader = input.readLine();
        }

    }

    // the init function
    private void init() {
        // clear the memory and registers
        for (char arr[] : this.memory) {
            Arrays.fill(arr, ' ');
        }

        this.instructionCounter = 0;
        Arrays.fill(this.aRegister, ' ');
        Arrays.fill(this.instructionRegister, ' ');
        this.toggleRegister = false;

        // clear the contents of the card
        Arrays.fill(this.cardReader, false);

        // initialise the values of interrupts and counters
        this.SI = 0;
        // this.EM = -1;
        this.PI = 0;
        this.TI = 0;
        this.PTR = 0;
        this.TLC = 0;
        this.TTC = 0;
        this.realAddress = 0;
        this.allocateList.clear();
        this.PTR = allocate() * 10;

        // initialization of Page Table
        for (int i = PTR; i < PTR + 10; i++) {
            memory[i][0] = '0';
            memory[i][2] = '*';
            memory[i][3] = '*';
        }

        // initialise PCB and page table register
        ptrPointer = PTR;
        this.processBoard = new PCB(0, 0, 0);

        // map to map virtual address to real address
        this.theMap.clear();
        count = 0;
    }

    // function to Print Memory
    private void printMemory() {
        for (int i = 0; i < memory.length; i++) {
            System.out.println(i + " " + Arrays.toString(memory[i]));
        }
    }

    // function startExecution program
    private void startExecution() throws Exception {
        // set the instruction counter to 0 and call the executeUserProgram
        this.instructionCounter = 0;
        executeUserProgram();
    }

    // 5. executeUserProgram

    private void executeUserProgram() throws Exception {
        boolean loop = true;
        while (loop) {
            addressMap(instructionCounter);
            // loading Instruction in IR
            int j = 0;
            for (char i : memory[realAddress]) {
                instructionRegister[j] = i;
                j++;
            }
            // Increment Instruction Counter by 1
            instructionCounter = instructionCounter + 1;

            // fetch the operand from 3rd and 4th bit
            int operand = (int) (instructionRegister[2] - '0') * 10 + (int) (instructionRegister[3] - '0');
            System.out.println("operand" + operand);

            // map logical address to physical address
            map(operand);

            operand = realAddress;
            System.out.println("operand" + operand);

            // Separating Operand and Opcode
            StringBuilder opcode = new StringBuilder();
            if (instructionRegister[0] == 'H') {
                opcode.append(instructionRegister[0]);
            } else {
                opcode.append(instructionRegister[0]);
                opcode.append(instructionRegister[1]);
            }

            System.out.println("Opcode  : " + opcode);
            System.out.println("Operand  : " + operand);

            // examine the opcode
            switch (opcode.toString()) {
                case "GD":
                    SI = 1;
                    break;
                case "PD":
                    SI = 2;
                    break;
                case "H":
                    SI = 3;
                    loop = false;
                    break;
                case "LR":
                    loadRegister(operand);
                    break;
                case "SR":
                    storeRegister(operand);
                    break;
                case "CR":
                    compareRegister(operand);
                    break;
                case "BT":
                    branchOnTrue(operand);
                    break;
                default:
                    System.out.println("Invalide Command Or Command Not Found");
                    PI = 1;
                    break;
            }
            simulation();
            if (SI != 0 || PI != 0 || TI != 0) {
                System.out.println("SI = " + SI);
                System.out.println("PI = " + PI);
                System.out.println("TI = " + TI);
                if (mos(operand) == -1) {
                    loop = false;
                }
                SI = 0;
                PI = 0;
                TI = 0;

            }

        }

    }

    private void branchOnTrue(int operand) {
        if (toggleRegister) {
            this.instructionCounter = operand;
            System.out.println("IC = " + instructionCounter);
        }
    }

    private void compareRegister(int operand) {
        int c = 0;
        int j = 0;
        for (char i : aRegister) {
            if (memory[operand][j] == i) {
                c++;
            }
            j++;
        }
        System.out.println(c);
        if (c == 4) {
            this.toggleRegister = true;
        }
    }

    private void storeRegister(int operand) {
        int j = 0;
        for (char i : aRegister) {
            memory[operand][j] = i;
            j++;
        }

    }

    private void loadRegister(int operand) {
        int j = 0;
        for (char i : memory[operand]) {
            aRegister[j] = i;
            j++;
        }
        System.out.println(Arrays.toString(aRegister));
    }

    private void simulation() {
        TTC++;
        System.out.println("TTC = " + TTC);

        if (TTC > processBoard.TTL) {
            TI = 2;
        }
        System.out.println(TI);
    }

    private int mos(int operand) throws Exception {
        if (TI == 0) {
            if (PI == 1) {
                terminate(4);
                return -1;
            } else if (PI == 2) {
                terminate(5);
                return -1;
            } else if (PI == 3) {
                terminate(6);
                return -1;
            } else if (SI == 1) {
                return READ(operand);
            } else if (SI == 2) {
                return WRITE(operand);
            } else if (SI == 3) {
                terminate(0);
                return -1;
            }
        } else if (TI == 2) {
            if (PI == 1) {
                terminate(8);
                return -1;
            } else if (PI == 2) {
                terminate(7);
                return -1;
            } else if (PI == 3) {
                terminate(6);
                return -1;
            } else if (SI == 1) {
                terminate(3);
                return -1;
            } else if (SI == 2) {
                WRITE(operand);
                terminate(3);
                return -1;
            } else if (SI == 3) {
                terminate(0);
                return -1;
            } else {
                terminate(3);
                return -1;
            }
        }

        return 1;

    }

    private int READ(int location) throws Exception {
        String Data = input.readLine();
        if (Data.contains("$END")) {
            terminate(1);
            return -1;
        } else {
            System.out.println(Data);
            int col = 0;
            for (char i : Data.toCharArray()) {
                memory[location][col % 4] = i;
                col++;
                if (col % 4 == 0) {
                    location++;
                }
                if (location > 299) {
                    System.out.println("Memory Exceed! " + location);
                    break;
                }
            }
        }
        return 1;
    }

    private int WRITE(int location) throws Exception {
        TLC++;
        System.out.println("TLC = " + TLC);
        if (TLC > processBoard.TLL) {
            terminate(2);
            return -1;
        } else {
            int col = 0;
            char i = memory[location][col];
            StringBuilder Data = new StringBuilder();
            int j = location;
            while (j < location + 10) {
                Data.append(i);
                col++;
                if (col % 4 == 0) {
                    j++;
                }
                if (j > 299) {
                    System.out.println("Memory Exceed! " + j);
                    break;
                }
                i = memory[j][col % 4];
            }
            output.write(Data.toString());
            output.newLine();
        }

        return 1;
    }

    private void terminate(int EM) throws Exception {

        String error = "";
        switch (EM) {
            case 0:
                error = "No Error";
                break;
            case 1:
                error = "Out of Data";
                break;
            case 2:
                error = "Line Limit Exceeded";
                break;
            case 3:
                error = "Time Limit Exceeded";
                break;
            case 4:
                error = "Operation Code Error";
                break;
            case 5:
                error = "Operand Error";
                break;
            case 6:
                error = "Invalid Page Fault";
                break;
            case 7:
                error = "Time Limit Exceed + Operand Error";
                break;
            case 8:
                error = "Time Limit Exceed + Operation Code Error";
                break;
            default:
                System.out.println("Invalide Error Message");
        }

        // Convert IR array to string without spaces and brackets
        String irString = "";
        for (char c : instructionRegister) {
            irString += (c == ' ' ? ' ' : c);
        }

        // Write output with consistent formatting
        output.write(String.format("JOB ID   :  %d%n", processBoard.JID));
        output.write(error + "\n");
        output.write(String.format("IC       :  %d%n", instructionCounter));
        output.write(String.format("IR       :  %-4s%n", irString));
        output.write(String.format("TTC      :  %d%n", TTC));
        output.write(String.format("LLC      :  %d%n", TLC));
        output.write("\n\n");
    }

    // function to generate random number
    private int allocate() {
        Random rand = new Random();
        int value;

        // Generate a random value between 0 and 29 and ensure it is not in the list
        do {
            value = rand.nextInt(30); // Generates a number between 0 and 29
        } while (allocateList.contains(value));

        // Add the value to the allocateList to track it
        allocateList.add(value);
        return value;
    }

    // printing PTR
    private void printPageTable(int ptr) {
        System.out.println("Page Table");
        for (int i = ptr; i < ptr + 10; i++) {
            System.out.println(i + " " + Arrays.toString(memory[i]));
        }
    }

    // printing PCB
    private void printPCB(PCB processBoard) {
        System.out.println("JID : " + processBoard.JID);
        System.out.println("TTL : " + processBoard.TTL);
        System.out.println("TLL : " + processBoard.TLL);
    }

    // Address Map
    private void addressMap(int IC) {
        if (IC % 10 == 0 && IC != 0) {
            count++;
        }
        int address = (int) (memory[PTR + count][2] - '0') * 10 + (int) (memory[PTR + count][3] - '0');

        address = address * 10 + IC % 10;

        realAddress = address;

    }

    // Add into MAP
    private void map(int add) {
        if ((int) (instructionRegister[2] - '0') < 0 || (int) (instructionRegister[2] - '0') > 9
                || (int) (instructionRegister[3] - '0') < 0 || (int) (instructionRegister[3] - '0') > 9) {
            if (instructionRegister[0] != 'H') {
                PI = 2;
                return;
            }
            realAddress = -1;
            return;
        }
        if (instructionRegister[0] == 'B' && instructionRegister[1] == 'T') {
            System.out.println("This is BT");
            realAddress = add;
            return;
        }

        if (theMap.containsKey((add / 10) * 10)) {
            realAddress = theMap.get((add / 10) * 10) * 10 + (add % 10);
            System.out.println("from map");
            return;
        }
        if ((instructionRegister[0] == 'G' && instructionRegister[1] == 'D')
                || (instructionRegister[0] == 'S' && instructionRegister[1] == 'R')) {
            int temp = allocate();
            theMap.put(add, temp);
            // printf("allocated memory block is %d for %d operand\n\n\n",value[key_index]
            // ,key[key_index]);
            memory[ptrPointer][0] = '1';
            memory[ptrPointer][3] = (char) (temp % 10 + '0');
            memory[ptrPointer][2] = (char) (temp / 10 + '0');
            ptrPointer++;
            realAddress = theMap.get(add) * 10;
        } else {
            PI = 3;
            return;
        }
    }

    public static void main(String[] args) throws Exception {
        String InputFile = "input_phase2.txt";
        String OutputFile = "output.txt";

        dusraPhase2 processBoard = new dusraPhase2(InputFile, OutputFile);

        processBoard.init();
        processBoard.load();
        processBoard.output.close();
    }
}
