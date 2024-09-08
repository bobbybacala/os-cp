// Initial version but not working

package phase1;
import java.io.*;
import java.util.*;

public class VirtualMachine {
    // Constants
    private static final int MEMORY_BLOCK_SIZE = 10; // Number of words in a block
    private static final int MEMORY_SIZE = 100; // Total number of words in memory
    private static final int WORD_SIZE = 4; // Number of bytes in a word

    // CPU registers
    private String instructionRegister = ""; // 4 bytes
    private int instructionCounter = 0; // 2 bytes
    private boolean toggleRegister = false; // 1 byte (True/False)

    // Main memory as 2D matrix [100][4]
    private char[][] memory = new char[MEMORY_SIZE][WORD_SIZE];

    // Input and output files
    private String inputFileName = "input.txt";
    private String outputFileName = "output.txt";
    private BufferedWriter outputWriter;

    // Constructor
    public VirtualMachine() {
        try {
            outputWriter = new BufferedWriter(new FileWriter(outputFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to load input file and execute the instructions
    public void run() {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFileName))) {
            String line;
            boolean isDataSection = false;
    
            while ((line = br.readLine()) != null) {
                if (line.startsWith("$AMJ")) {
                    initializeJob(line);
                } else if (line.startsWith("$DTA")) {
                    isDataSection = true;
                } else if (line.startsWith("$END")) {
                    endJob();
                } else {
                    if (isDataSection) {
                        loadDataCard(line);
                    } else {
                        executeProgramCard(line);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    private void initializeJob(String jobCard) {
        // Job initialization logic (parse job details, etc.)
        instructionCounter = 0; // Reset instruction counter for the new job
        System.out.println("Job initialized: " + jobCard);
    }

    private void loadDataCard(String dataCard) {
        // Load data into memory from the data card
        System.out.println("Loading data card: " + dataCard); // Debug statement
        String[] dataWords = dataCard.split(" ");
        for (String word : dataWords) {
            if (instructionCounter >= MEMORY_SIZE) {
                System.out.println("Memory overflow error.");
                return;
            }
            for (int i = 0; i < WORD_SIZE && i < word.length(); i++) {
                memory[instructionCounter][i] = word.charAt(i);
            }
            // Fill remaining spaces with spaces to complete the word size
            for (int i = word.length(); i < WORD_SIZE; i++) {
                memory[instructionCounter][i] = ' ';
            }
            instructionCounter++;
        }
        printMemoryState(); // Debug: Print memory after loading data card
    }

    private void executeProgramCard(String programCard) {
        // Split the program card into instructions
        List<String> instructions = new ArrayList<>();
        for (int i = 0; i < programCard.length(); i += 4) {
            if (i + 4 <= programCard.length()) {
                instructions.add(programCard.substring(i, i + 4));
            }
        }

        // Execute each instruction
        for (String instr : instructions) {
            instructionRegister = instr; // Load instruction register
            decodeAndExecute(instr);
        }
    }

    private void decodeAndExecute(String instruction) {
        String operation = instruction.substring(0, 2);
        int operand = 0;
        if (instruction.length() > 2) {
            operand = Integer.parseInt(instruction.substring(2));
        }

        switch (operation) {
            case "GD":
                getData(operand);
                break;
            case "PD":
                putData(operand);
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
                branchTrue(operand);
                break;
            case "H":
                halt();
                break;
            default:
                System.out.println("Invalid instruction: " + instruction);
                break;
        }
    }

    private void getData(int startAddress) {
        System.out.println("Getting data into block starting at address: " + startAddress);
    
        try (BufferedReader br = new BufferedReader(new FileReader(inputFileName))) {
            String line;
            boolean isDataSection = false;
            int memoryPosition = startAddress;
    
            while ((line = br.readLine()) != null) {
                if (line.startsWith("$DTA")) {
                    isDataSection = true;
                    continue; // Skip the control card itself
                }
    
                if (isDataSection) {
                    String[] dataWords = line.split(" ");
                    for (String word : dataWords) {
                        if (memoryPosition >= MEMORY_SIZE) {
                            System.out.println("Memory overflow error.");
                            return;
                        }
                        for (int i = 0; i < WORD_SIZE && i < word.length(); i++) {
                            memory[memoryPosition][i] = word.charAt(i);
                        }
                        // Fill remaining spaces with spaces to complete the word size
                        for (int i = word.length(); i < WORD_SIZE; i++) {
                            memory[memoryPosition][i] = ' ';
                        }
                        memoryPosition++;
                    }
                    break; // Stop after loading all data after $DTA into memory
                }
            }
            printMemoryState(); // Debug: Print memory after loading data
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    
    

    private void putData(int blockNumber) {
        System.out.println("Putting data from block starting at address: " + blockNumber);
        try {
            int startAddress = blockNumber;
            StringBuilder outputLine = new StringBuilder(); // To collect all words into a single line
            
            for (int i = startAddress; i < startAddress + MEMORY_BLOCK_SIZE && i < MEMORY_SIZE; i++) {
                StringBuilder word = new StringBuilder();
                for (int j = 0; j < WORD_SIZE; j++) {
                    word.append(memory[i][j]);
                }
                // Append the word to output line, trimming any extra spaces
                outputLine.append(word.toString().trim()).append(" ");
            }
            
            outputWriter.write(outputLine.toString().trim()); // Write the collected output to file
            outputWriter.newLine(); // Ensure each output is on a new line
            outputWriter.flush();   // Ensure data is written to the file
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    private void loadRegister(int address) {
        // Validate the address to ensure it's within memory bounds
        if (address < 0 || address >= MEMORY_SIZE) {
            System.out.println("Invalid memory address: " + address);
            return;
        }
    
        // Initialize the instruction register as an empty string
        instructionRegister = "";
    
        // Load the 4-byte word from memory at the given address into the instruction register
        for (int i = 0; i < WORD_SIZE; i++) {
            instructionRegister += memory[address][i];
        }
    
        System.out.println("Instruction Register loaded with: " + instructionRegister);
    }
    

    private void storeRegister(int address) {
        // Store data from CPU register to memory
        System.out.println("Storing register to address: " + address);
        // to be implemented
    }

    private void compareRegister(int address) {
        // Compare register data with memory data
        System.out.println("Comparing register with address: " + address);
        // to be implemented
    }

    private void branchTrue(int address) {
        // Branch to specific address if toggle register is true
        if (toggleRegister) {
            System.out.println("Branching to address: " + address);
            instructionCounter = address;
        }
    }

    private void halt() {
        // Halt execution
        System.out.println("Halting execution.");
        try {
            if (outputWriter != null) {
                outputWriter.newLine(); // Add newline at the end of the file
                outputWriter.flush();
                outputWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void endJob() {
        // End job and reset machine state if needed
        System.out.println("Job ended.");
        try {
            if (outputWriter != null) {
                outputWriter.flush();
                outputWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Debug method to print the current state of memory
    private void printMemoryState() {
        System.out.println("Current memory state:");
        for (int i = 0; i < MEMORY_SIZE; i++) {
            for (int j = 0; j < WORD_SIZE; j++) {
                System.out.print(memory[i][j]);
            }
            System.out.print(" ");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        VirtualMachine vm = new VirtualMachine();
        vm.run();
    }
}
