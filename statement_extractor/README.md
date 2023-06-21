# Statement Extractor

### Java program uses Ballerina's syntax tree API to load a Ballerina source file and extract variables defined in the file and make the data set.

   ##### 1) The program reads a Ballerina source file and extracts information about the variables and the statements in which they are used.

   ##### 2) The program uses the Ballerina compiler syntax tree and visitor pattern to extract the information.

   #####  3) The extracted information is written to a CSV file in the format of "variableLabel,statementSourceCode".

   #####  4) The program first checks if the file path is empty, and returns an error message if it is. The program then reads the file content into a string, creates a TextDocument, and converts it into a SyntaxTree.

   #####  5) The syntax tree is then visited by an instance of the StatementVisitor class, which retrieves the variable names and the statements in which they are used, and stores them in two separate lists.

   #####  6) The writeToCSV method is then called to write the information to a CSV file.
