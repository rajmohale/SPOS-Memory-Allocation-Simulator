# Dynamic Memory Allocation and Fragmentation Simulator

A Java 17 Swing desktop simulator for dynamic partition allocation. The application supports First Fit, Best Fit, and Worst Fit, process allocation/deallocation, adjacent free-block coalescing, memory statistics, algorithm comparison, operation history, step execution, reset, and CSV export.

## Requirements

- Java Development Kit (JDK) 17 or newer
- Apache Maven 3.8 or newer to run the test suite and package the application

## Run

From the project root:

```powershell
mvn clean package
java -jar target/memory-allocation-simulator-1.0.0.jar
```

To run without packaging:

```powershell
mvn compile
mvn exec:java -Dexec.mainClass="com.spos.memory.Main"
```

If Maven is not installed, compile the application with a JDK:

```powershell
New-Item -ItemType Directory -Force target/classes
javac --release 17 -d target/classes src/main/java/com/spos/memory/model/*.java src/main/java/com/spos/memory/service/*.java src/main/java/com/spos/memory/ui/*.java src/main/java/com/spos/memory/Main.java
java -cp target/classes com.spos.memory.Main
```

## Test

```powershell
mvn test
```

JUnit 5 tests cover fit selection, process validation, memory statistics, coalescing, and independent algorithm comparisons.

## Use

- Select an allocation strategy and total memory in the header. Reset applies the configured total and clears active allocations, queued requests, and comparison results.
- Enter a process ID and positive size, then choose **Allocate** for immediate allocation or **Queue** to add an allocation request.
- **Step** executes one queued request. **Run queue** executes all queued requests in order.
- Enter an allocated process ID and choose **Deallocate**. Adjacent free blocks are merged automatically.
- The **Compare** tab runs the queued requests against independent copies of the current memory map using all three strategies.
- The **History** tab records successful and failed operations. **Export CSV** saves current memory statistics, memory blocks, and history.

Memory addresses are zero-based and block end addresses are exclusive. External fragmentation is reported as `free memory - largest free block`; its percentage is measured relative to total free memory.
