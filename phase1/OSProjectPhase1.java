package phase1;

import java.io.*;

public class OSProjectPhase1 {

    // components of virtual memory

    public static char[] instructionRegister = new char[4]; // Instruction Register (IR)
    public static char[] aRegister = new char[4]; // Accumulator Register (R)
    public static int instructionCounter = 0; // Instruction Counter (IC)
    public static boolean toggleRegister = false; // Comparison Toggle Register (C)
    public static int systemInterrupt = 3; // System Interrupt (SI)
    public static char[][] memory = new char[100][4]; // Memory M[100][4]
    public static BufferedReader input; // Input reader
    public static FileWriter output; // Output writer

    // Initialize memory and registers
    private static void init() {
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 4; j++) {
                memory[i][j] = ' ';
            }
        }
        for (int i = 0; i < 4; i++) {
            instructionRegister[i] = ' ';
            aRegister[i] = ' ';
        }
        instructionCounter = 0;
        toggleRegister = false;
        systemInterrupt = 3; // Set default interrupt for halt
    }

    // Fetch the instruction from memory
    private static void fetchInstruction() {
        for (int i = 0; i < 4; i++) {
            instructionRegister[i] = memory[instructionCounter][i]; // Fetch the instruction from memory[IC]
        }
        instructionCounter++; // Increment the instruction counter after fetching
    }

    // Start execution by setting IC to 0
    private static void startExecution() throws IOException {
        instructionCounter = 0;
        executeUserProgram();
    }

    // Execute the loaded program
    private static void executeUserProgram() throws IOException {
        while (true) {
            fetchInstruction(); // Fetch the next instruction
            String opcode = "" + instructionRegister[0] + instructionRegister[1]; // Extract opcode

            switch (opcode) {
                case "LR":
                    loadRegister();
                    break;
                case "SR":
                    storeRegister();
                    break;
                case "CR":
                    compareRegister();
                    break;
                case "BT":
                    branchOnTrue();
                    break;
                case "GD":
                    systemInterrupt = 1;
                    mos();
                    break;
                case "PD":
                    systemInterrupt = 2;
                    mos();
                    break;
                case "H ":
                    systemInterrupt = 3;
                    mos();
                    return;
                default:
                    System.out.println("Invalid opcode: " + opcode);
                    return; // Abort execution on invalid opcode
            }
        }
    }

    // Load data from memory into the accumulator register R
    private static void loadRegister() {
        int address = (instructionRegister[2] - '0') * 10 + (instructionRegister[3] - '0');
        if (address >= 100) {
            System.out.println("Address out of bounds during load.");
            return;
        }
        for (int i = 0; i < 4; i++) {
            aRegister[i] = memory[address][i]; // Load memory data into R
        }
        System.out.println("Loaded into R from memory[" + address + "]: " + String.valueOf(aRegister));
    }

    // Store data from accumulator register R into memory
    private static void storeRegister() {
        int address = (instructionRegister[2] - '0') * 10 + (instructionRegister[3] - '0');
        if (address >= 100) {
            System.out.println("Address out of bounds during store.");
            return;
        }
        for (int i = 0; i < 4; i++) {
            memory[address][i] = aRegister[i]; // Store R data into memory
        }
        System.out.println("Stored R into memory[" + address + "]: " + String.valueOf(aRegister));
    }

    // Compare contents of register R with memory
    private static void compareRegister() {
        int address = (instructionRegister[2] - '0') * 10 + (instructionRegister[3] - '0');
        if (address >= 100) {
            System.out.println("Address out of bounds during compare.");
            return;
        }
        toggleRegister = true; // Assume equality
        for (int i = 0; i < 4; i++) {
            if (aRegister[i] != memory[address][i]) {
                toggleRegister = false; // Set false if any byte mismatches
                break;
            }
        }
        System.out.println("Compared R with memory[" + address + "], toggleRegister = " + toggleRegister);
    }

    // Branch to memory address if toggle register C is true
    private static void branchOnTrue() {
        if (toggleRegister) {
            int address = (instructionRegister[2] - '0') * 10 + (instructionRegister[3] - '0');
            if (address >= 100) {
                System.out.println("Address out of bounds during branch.");
                return;
            }
            instructionCounter = address; // Set instruction counter to branch address
            System.out.println("Branching to address: " + address);
        } else {
            System.out.println("Branch not taken.");
        }
    }

    // Read data from input file and store in memory
    private static void read() throws IOException {
        String buffer = input.readLine();
        if (buffer == null || buffer.equals("$END")) {
            System.out.println("Out of data.");
            return;
        }
        // if user enters GD43, PD56, it is illegal , hence we handle this on the kernel
        // side by making the last bit 0
        // Since GD, PD work Block wise
        instructionRegister[3] = 0;
        
        // TAKE ONLY THE SECOND LAST BIT AND MULTIPLY IT BY 10, SINCE GD, PD
        // instructions work blockwise
        int address = (instructionRegister[2] - '0') * 10;
        if (address >= 100) {
            System.out.println("Address out of bounds during read.");
            return;
        }
        int k = 0;
        for (int i = 0; i < buffer.length() && i < 40; i++) {
            memory[address][k++] = buffer.charAt(i);
            if (k == 4) {
                k = 0;
                address++;
                if (address >= 100)
                    break;
            }
        }
        System.out.println("Data read into memory.");
    }

    // Write memory contents to output file
    private static void write() throws IOException {
        // TAKE ONLY THE SECOND LAST BIT AND MULTIPLY IT BY 10, SINCE GD, PD
        // instructions work blockwise

        // if user enters GD43, PD56, it is illegal , hence we handle this on the kernel
        // side by making the last bit 0
        // Since GD, PD work Block wise
        instructionRegister[3] = 0;
        int address = (instructionRegister[2] - '0') * 10;

        if (address >= 100) {
            System.out.println("Address out of bounds during write.");
            return;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 10 && address < 100; i++) {
            for (int j = 0; j < 4; j++) {
                out.append(memory[address][j]);
            }
            address++;
        }
        output.write(out.toString().trim() + '\n');
        System.out.println("Data written from memory to output.");
    }

    // Terminate the program
    private static void terminate() throws IOException {
        output.write('\n');
        output.write('\n');
        System.out.println("Program terminated.");
    }

    // Memory Operating System (MOS) to handle interrupts
    private static void mos() throws IOException {
        switch (systemInterrupt) {
            case 1:
                read();
                break;
            case 2:
                write();
                break;
            case 3:
                terminate();
                break;
            default:
                System.out.println("Unknown interrupt.");
        }
    }

    // Load jobs into memory from the input file
    private static void load() throws IOException {
        String buffer;
        int m = 0;
        while ((buffer = input.readLine()) != null) {
            if (buffer.startsWith("$AMJ")) {
                init();
                m = 0;
                System.out.println("New job found.");
            } else if (buffer.startsWith("$DTA")) {
                startExecution();
            } else if (buffer.startsWith("$END")) {
                System.out.println("End of job.");
            } else {
                int k = 0;
                for (int i = m; i < m + 10 && k < buffer.length() && i < 100; i++) {
                    for (int j = 0; j < 4 && k < buffer.length(); j++) {
                        memory[i][j] = buffer.charAt(k++);
                    }
                }
                m += 10;
            }
        }
    }

    // Print the current memory contents
    private static void printMemory() {
        System.out.println("Memory content:");
        for (int i = 0; i < 100; i++) {
            System.out.print("M[" + i + "]: ");
            for (int j = 0; j < 4; j++) {
                System.out.print(memory[i][j]);
            }
            System.out.println();
        }
    }

    // Main function to initialize and start the OS simulation
    public static void main(String[] args) {
        try {
            input = new BufferedReader(new FileReader(
                    "D:/Study/Codes/VS projects/Java Projects/Operating System/os-cp/phase1/input.txt"));
            output = new FileWriter(
                    "D:/Study/Codes/VS projects/Java Projects/Operating System/os-cp/phase1/output.txt");
            load(); // Load the jobs from input
            printMemory(); // Print memory contents after load
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null)
                    input.close();
                if (output != null)
                    output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
