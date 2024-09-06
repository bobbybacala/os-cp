import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class OSProjectPhase1 {

    // Memory M[100][4], where 1 word = 4 bytes
    private static String[][] memory = new String[100][4];

    // 4-byte instruction register
    private static char[] instructionRegister = new char[4];

    // 4-byte A register
    private static char[] aRegister = new char[4];

    // 2-byte instruction counter
    private static short[] instructionCounter = new short[2];

    // Toggle register
    private static boolean toggleRegister;

    // System interrupt
    private static int systemInterrupt;

    // Initialize all components
    public void init() {
        // Initialize memory
        for (String[] row : memory) {
            Arrays.fill(row, "    "); // 4 spaces for each word
        }

        // Initialize registers and counters
        Arrays.fill(instructionRegister, '\0'); // Null char for instructionRegister
        Arrays.fill(aRegister, '\0'); // Null char for aRegister
        Arrays.fill(instructionCounter, (short) 0); // Instruction counter set to 0

        // Initialize toggle register
        toggleRegister = false;

        // Initialize system interrupt
        systemInterrupt = 3;
    }

    // Load function for Phase 1
    public void load(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;

            // we start initially at index 0 of the memory
            int memoryIndex = 0;
            boolean dataSection = false;

            while ((line = br.readLine()) != null) {
                // If control card is found, handle it
                if (line.startsWith("$AMJ")) {
                    System.out.println("Starting a new job: " + line.substring(4));
                } else if (line.startsWith("$DTA")) {
                    System.out.println("Control card $DTA found");
                    dataSection = true; // Call mosStartExecution() at this point
                    mosStartExecution();
                } else if (line.startsWith("$END")) {
                    System.out.println("Job ended: " + line.substring(4));
                } else if (!dataSection) {
                    // Handle program card, store it in memory in blocks of 10 words
                    String[] programWords = parseProgramCard(line);

                    for (String word : programWords) {
                        if (memoryIndex >= 100) {
                            System.out.println("Memory overflow, aborting.");
                            return; // If memory is full, abort the process
                        }
                        // Storing word in memory (each word occupies a row in memory)
                        storeInMemory(word, memoryIndex);
                        memoryIndex++;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        printMemory();
    }

    // Parse the program card into 4-byte words
    private String[] parseProgramCard(String line) {
        int length = line.length();
        int numWords = (length + 3) / 4; // Each word is 4 bytes

        String[] words = new String[numWords];

        for (int i = 0; i < numWords; i++) {
            int start = i * 4;
            int end = Math.min(start + 4, length); // Handle the last word which might be less than 4 bytes
            words[i] = line.substring(start, end);
        }

        return words;
    }

    // Store a word in memory
    private void storeInMemory(String word, int index) {
        for (int i = 0; i < 4; i++) {
            if (i < word.length()) {
                memory[index][i] = String.valueOf(word.charAt(i));
            } else {
                memory[index][i] = " "; // Padding with spaces if word is less than 4 bytes
            }
        }
    }

    // Print the current state of memory
    private void printMemory() {
        System.out.println("Memory content:");
        for (int i = 0; i < 100; i++) {
            // if (Arrays.equals(memory[i], new String[]{" ", " ", " ", " "})) {
            // continue; // Skip empty memory slots
            // }
            System.out.print("M[" + i + "]: ");
            for (int j = 0; j < 4; j++) {
                System.out.print(memory[i][j]);
            }
            System.out.println();
        }
    }

    // MOS/Start Execution: Set Instruction Counter to 0 and call executeUserPrg()
    public void mosStartExecution() {
        // Set instruction counter to 00
        instructionCounter[0] = 0;
        instructionCounter[1] = 0;

        // Call executeUserPrg to start program execution
        executeUserPrg();
    }

    // Execute the user program
    public void executeUserPrg() {
        // Execution loop (Slave Mode)
        while (true) {
            // Fetch instruction from memory based on the instruction counter
            fetchInstruction();

            // Increment the instruction counter to point to the next instruction
            incrementInstructionCounter();

            // Examine opcode (first 2 bytes of the instruction register)
            String opcode = "" + instructionRegister[0] + instructionRegister[1];

            // Switch case to handle different instructions
            switch (opcode) {
                case "LR": // Load Register
                    loadRegister();
                    break;
                case "SR": // Store Register
                    storeRegister();
                    break;
                case "CR": // Compare Register
                    compareRegister();
                    break;
                case "BT": // Branch on True
                    branchOnTrue();
                    break;
                case "GD": // Get Data (Input)
                    systemInterrupt = 1;
                    mos();
                    break;
                case "PD": // Put Data (Output)
                    systemInterrupt = 2;
                    mos();
                    break;
                case "H ": // Halt
                    systemInterrupt = 3;
                    mos();
                    return; // End program
                default:
                    System.out.println("Invalid opcode: " + opcode);
                    return; // Abort execution on invalid opcode
            }
        }
    }

    // Fetch instruction from memory and load into instruction register
    // private void fetchInstruction() {
    // int memoryIndex = instructionCounter[0] * 10 + instructionCounter[1]; //
    // Convert instruction counter to memory
    // // index
    // instructionRegister = convertStringArrayToCharArray(memory[memoryIndex]);
    // }

    private void fetchInstruction() {
        int memoryIndex = instructionCounter[0] * 10 + instructionCounter[1];
        char[] instruction = new char[4];
        for (int i = 0; i < 4; i++) {
            instruction[i] = memory[memoryIndex][i].charAt(0); // Extract individual characters
        }
        instructionRegister = instruction;
    }

    // Increment the instruction counter
    private void incrementInstructionCounter() {
        instructionCounter[1]++;
        if (instructionCounter[1] == 10) { // Move to the next row of memory
            instructionCounter[0]++;
            instructionCounter[1] = 0;
        }
    }

    // Load register (LR: R <- M[IR[3,4]])
    private void loadRegister() {
        int memoryAddress = getMemoryAddress();
        aRegister = convertStringArrayToCharArray(memory[memoryAddress]);
    }

    // Store register (SR: R -> M[IR[3,4]])
    private void storeRegister() {
        int memoryAddress = getMemoryAddress();
        memory[memoryAddress] = convertCharArrayToStringArray(aRegister);
    }

    // Compare register (CR: Compare R and M[IR[3,4]])
    private void compareRegister() {
        int memoryAddress = getMemoryAddress();
        toggleRegister = Arrays.equals(aRegister, convertStringArrayToCharArray(memory[memoryAddress]));
    }

    // Branch on true (BT: If C = T then IC <- IR[3,4])
    private void branchOnTrue() {
        if (toggleRegister) {
            int memoryAddress = getMemoryAddress();
            instructionCounter[0] = (short) (memoryAddress / 10);
            instructionCounter[1] = (short) (memoryAddress % 10);
        }
    }

    // Helper to calculate memory address from IR[3,4] (3rd and 4th bytes of IR)
    private int getMemoryAddress() {
        int address = (instructionRegister[2] - '0') * 10 + (instructionRegister[3] - '0'); // Convert IR[3,4] to
                                                                                            // integer address
        return address;
    }

    // Convert a String array (from memory) to a char array (for registers)
    private char[] convertStringArrayToCharArray(String[] strArray) {
        char[] charArray = new char[4];
        for (int i = 0; i < 4; i++) {
            charArray[i] = strArray[i].charAt(0);
        }
        return charArray;
    }

    // Convert a char array (from registers) to a String array (for memory)
    private String[] convertCharArrayToStringArray(char[] charArray) {
        String[] strArray = new String[4];
        for (int i = 0; i < 4; i++) {
            strArray[i] = String.valueOf(charArray[i]);
        }
        return strArray;
    }

    // MOS (handling interrupts)
    private void mos() {
        switch (systemInterrupt) {
            case 1:
                // System Interrupt: Get Data (GD)
                System.out.println("System Interrupt: Get Data (GD)");
                read();
                break;
            case 2:
                // System Interrupt: Put Data (PD)
                System.out.println("System Interrupt: Put Data (PD)");
                // write();
                break;
            case 3:
                // System Interrupt: Halt (H)
                System.out.println("System Interrupt: Halt (H)");
                // terminate();
                break;
            default:
                System.out.println("Unknown interrupt.");
                break;
        }
    }

    private void terminate() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'terminate'");
    }

    private void write() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'write'");
    }

    private void read() {
        instructionRegister[3] = '0'; // IR[4] <- 0 to handle proper block reading

        // Calculate the starting memory address from IR[3,4]
        int memoryStartIndex = getMemoryAddress();

        // Read the next data card from the input file and store it in memory
        try (BufferedReader br = new BufferedReader(new FileReader("input.txt"))) {
            String line;
            boolean dataSection = false;

            // Look for the $DTA section and then read data cards
            while ((line = br.readLine()) != null) {
                if (line.startsWith("$DTA")) {
                    dataSection = true;
                    continue; // Start reading data after $DTA
                }

                if (dataSection) {
                    // If $END is reached, abort reading data
                    if (line.startsWith("$END")) {
                        System.out.println("Out-of-data: Reached $END");
                        return;
                    }

                    // Parse and store the data in memory starting at memoryStartIndex
                    String[] dataWords = parseProgramCard(line);
                    for (int i = 0; i < dataWords.length && memoryStartIndex < 100; i++) {
                        storeInMemory(dataWords[i], memoryStartIndex);
                        memoryStartIndex++;

                        // Stop after storing a block (10 words)
                        if ((memoryStartIndex - getMemoryAddress()) >= 10) {
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Continue program execution
        executeUserPrg();
    }

    public static void main(String[] args) {
        OSProjectPhase1 os = new OSProjectPhase1();
        os.init();
        os.load("input.txt");

        // debug statement to check the contents of some components of virtual machine
        // System.out.println(Arrays.toString(instructionCounter));
        System.out.println(Arrays.toString(aRegister));
        // System.out.println(Arrays.toString(memory[35]));
    }
}

