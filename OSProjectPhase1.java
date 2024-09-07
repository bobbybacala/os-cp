import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
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

    // Load function to load jobs into memory and process them
    public void load(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            int memoryIndex = 0;
            boolean dataSection = false;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("$AMJ")) {
                    System.out.println("Starting new job: " + line.substring(4));
                    memoryIndex = 0;
                    clearMemory(); // Clear memory for each new job
                } else if (line.startsWith("$DTA")) {
                    System.out.println("Control card $DTA found");
                    dataSection = true; // After this, we start reading data
                    mosStartExecution(); // Start executing the program
                } else if (line.startsWith("$END")) {
                    System.out.println("Job ended: " + line.substring(4));
                } else if (!dataSection) {
                    // Store program card in memory
                    String[] programWords = parseProgramCard(line);
                    for (String word : programWords) {
                        if (memoryIndex >= 100) {
                            System.out.println("Memory overflow, aborting.");
                            return;
                        }
                        storeInMemory(word, memoryIndex++);
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
            int end = Math.min(start + 4, length);
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
                memory[index][i] = " ";
            }
        }
    }

    // Clear memory after each job
    private void clearMemory() {
        for (int i = 0; i < 100; i++) {
            Arrays.fill(memory[i], "    ");
        }
    }

    // Print the current state of memory
    private void printMemory() {
        System.out.println("Memory content:");
        for (int i = 0; i < 100; i++) {
            System.out.print("M[" + i + "]: ");
            for (int j = 0; j < 4; j++) {
                System.out.print(memory[i][j]);
            }
            System.out.println();
        }
    }

    // MOS/Start Execution
    public void mosStartExecution() {
        instructionCounter[0] = 0;
        instructionCounter[1] = 0;
        executeUserPrg();
    }

    // Execute the user program
    public void executeUserPrg() {
        while (true) {
            fetchInstruction();
            incrementInstructionCounter();

            String opcode = "" + instructionRegister[0] + instructionRegister[1];
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
                    return;
            }
        }
    }

    // instructions functions
    // Load Register (LR: R <- M[IR[3,4]])
    private void loadRegister() {
        int memoryAddress = getMemoryAddress(); // Get the memory address from IR[3,4]
        aRegister = convertStringArrayToCharArray(memory[memoryAddress]); // Load data from memory into the A register
        System.out.println("Loaded into A register from memory[" + memoryAddress + "]: " + Arrays.toString(aRegister));
    }

    // Store Register (SR: R -> M[IR[3,4]])
    private void storeRegister() {
        int memoryAddress = getMemoryAddress(); // Get the memory address from IR[3,4]
        memory[memoryAddress] = convertCharArrayToStringArray(aRegister); // Store A register data into memory
        System.out.println("Stored A register into memory[" + memoryAddress + "]: " + Arrays.toString(aRegister));
    }

    // Compare Register (CR: Compare R and M[IR[3,4]])
    private void compareRegister() {
        int memoryAddress = getMemoryAddress(); // Get the memory address from IR[3,4]
        toggleRegister = Arrays.equals(aRegister, convertStringArrayToCharArray(memory[memoryAddress])); // Compare A register with memory
        System.out.println("Compared A register with memory[" + memoryAddress + "], toggleRegister set to: " + toggleRegister);
    }

    // Branch on True (BT: If C = T then IC <- IR[3,4])
    private void branchOnTrue() {
        if (toggleRegister) { // If the comparison was true (toggleRegister is set)
            int memoryAddress = getMemoryAddress(); // Get the memory address from IR[3,4]
            instructionCounter[0] = (short) (memoryAddress / 10); // Set IC to the new memory address
            instructionCounter[1] = (short) (memoryAddress % 10);
            System.out.println("Branching to memory address: " + memoryAddress);
        } else {
            System.out.println("Branch not taken. Toggle register is false.");
        }
    }

    // helper methods
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

    // Fetch instruction from memory
    private void fetchInstruction() {
        int memoryIndex = instructionCounter[0] * 10 + instructionCounter[1];
        char[] instruction = new char[4];
        for (int i = 0; i < 4; i++) {
            instruction[i] = memory[memoryIndex][i].charAt(0);
        }
        instructionRegister = instruction;
    }

    // Increment the instruction counter
    private void incrementInstructionCounter() {
        instructionCounter[1]++;
        if (instructionCounter[1] == 10) {
            instructionCounter[0]++;
            instructionCounter[1] = 0;
        }
    }

    // MOS (handling interrupts)
    private void mos() {
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

    // Read function
    // Read function for MOS
    private void read() {
        instructionRegister[3] = '0'; // IR[4] <- 0

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
                    // If $END is reached, stop reading data
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

    // Write function to write data in blocks of 10 words to output file
    private void write() {
        instructionRegister[3] = '0';
        int memoryStartIndex = getMemoryAddress();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("output.txt", true))) {
            for (int i = 0; i < 10 && memoryStartIndex + i < 100; i++) {
                for (int j = 0; j < 4; j++) {
                    bw.write(memory[memoryStartIndex + i][j]);
                }
                bw.write(" "); // Space between words
            }
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Terminate function
    private void terminate() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("output.txt", true))) {
            bw.newLine(); // Add two blank lines after job output
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get memory address from IR
    private int getMemoryAddress() {
        return (instructionRegister[2] - '0') * 10 + (instructionRegister[3] - '0');
    }

    public static void main(String[] args) {
        OSProjectPhase1 os = new OSProjectPhase1();
        os.init();
        os.load("input.txt");
    }
}
