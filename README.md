# Patient Information Extractor - Java NLP Tool

A comprehensive Java application that uses Natural Language Processing (NLP) libraries to extract patient names, dates of birth, and claim IDs from medical text files.

## Features

- **Patient Name Extraction**: Uses Stanford CoreNLP or Apache OpenNLP for Named Entity Recognition (NER)
- **Date of Birth Extraction**: Supports multiple date formats (MM/DD/YYYY, DD-MM-YYYY, Month DD, YYYY, etc.)
- **Claim ID Extraction**: Identifies various claim ID patterns and medical record numbers
- **Multiple NLP Library Support**: Provides implementations for both Stanford CoreNLP and Apache OpenNLP
- **File Processing**: Reads and processes text files containing medical records

## Project Structure

```
patient-info-extractor/
├── src/main/java/
│   ├── PatientInformationExtractor.java    # Main Stanford CoreNLP version
│   ├── OpenNLPPatientExtractor.java        # Apache OpenNLP version  
│   ├── PatientInfo.java                    # Data class for results
│   └── TextPatterns.java                   # Utility class for regex patterns
├── src/main/resources/
│   └── models/                             # NLP model files
├── sample_medical_record.txt               # Sample test data
├── pom.xml                                 # Maven dependencies
└── README.md                               # This file
```

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- Internet connection (for downloading NLP models)

## Installation

### 1. Clone/Download the Project

```bash
git clone <repository-url>
cd patient-info-extractor
```

### 2. Install Dependencies

```bash
mvn clean install
```

### 3. Download NLP Models

#### For Stanford CoreNLP:
The Maven dependency will automatically download the required models.

#### For Apache OpenNLP:
Download the following model files from [OpenNLP Models](http://opennlp.sourceforge.net/models-1.5/):

- `en-sent.bin` (Sentence Detection)
- `en-token.bin` (Tokenization)
- `en-ner-person.bin` (Person Name Recognition)

Place these files in the `src/main/resources/models/` directory.

## Usage

### Option 1: Using Stanford CoreNLP (Recommended)

```bash
# Compile and run with sample text
mvn compile exec:java -Dexec.mainClass="PatientInformationExtractor"

# Run with a specific file
mvn compile exec:java -Dexec.mainClass="PatientInformationExtractor" -Dexec.args="path/to/your/textfile.txt"
```

### Option 2: Using Apache OpenNLP

```bash
# Compile and run with sample text
mvn compile exec:java -Dexec.mainClass="OpenNLPPatientExtractor"

# Run with a specific file  
mvn compile exec:java -Dexec.mainClass="OpenNLPPatientExtractor" -Dexec.args="path/to/your/textfile.txt"
```

### Option 3: Create Executable JAR

```bash
# Build executable JAR
mvn clean package

# Run the JAR
java -jar target/patient-info-extractor-1.0-SNAPSHOT-shaded.jar sample_medical_record.txt
```

## Code Examples

### Basic Usage

```java
// Using Stanford CoreNLP version
PatientInformationExtractor extractor = new PatientInformationExtractor();
PatientInfo info = extractor.extractPatientInfo("path/to/medical/record.txt");

// Display results
System.out.println("Patient Names: " + info.getPatientNames());
System.out.println("Dates of Birth: " + info.getDatesOfBirth());
System.out.println("Claim IDs: " + info.getClaimIds());
```

### Processing Text Directly

```java
String medicalText = """
    Patient: John Smith
    DOB: January 15, 1985
    Claim ID: CLM123456789
    """;

PatientInfo info = extractor.extractPatientInfo(medicalText);
```

## Supported Formats

### Date Formats
- MM/DD/YYYY (e.g., 01/15/1985)
- DD/MM/YYYY (e.g., 15/01/1985)
- YYYY-MM-DD (e.g., 1985-01-15)
- Month DD, YYYY (e.g., January 15, 1985)
- DD Month YYYY (e.g., 15 January 1985)

### Claim ID Patterns
- Claim ID: ABC123456789
- Claim Number: DEF987654321
- Claim No: GHI555444333
- Medical ID: 1234567890123
- Generic alphanumeric IDs

### Patient Name Contexts
- Patient Name: [Name]
- Patient: [Name]
- Mr./Mrs./Ms./Dr. [Name]
- Context-based extraction using NER

## Performance Notes

- **Stanford CoreNLP**: More accurate but requires more memory (~500MB)
- **Apache OpenNLP**: Lighter weight but may require additional model tuning
- **Processing Speed**: ~1000 words per second on modern hardware
- **Memory Usage**: 256MB minimum recommended

## Troubleshooting

### Common Issues

1. **OutOfMemoryError**
   ```bash
   export MAVEN_OPTS="-Xmx2g"
   mvn exec:java ...
   ```

2. **Model Files Not Found**
   - Ensure OpenNLP models are in `src/main/resources/models/`
   - Check file permissions

3. **No Results Found**
   - Verify text format and encoding (UTF-8 recommended)
   - Check if text contains the expected patterns

### Logging

Enable debug logging by adding to command line:
```bash
-Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

## Customization

### Adding New Date Patterns

Edit the `DATE_PATTERNS` array in the extractor classes:

```java
private static final String[] DATE_PATTERNS = {
    "\\b(your-custom-pattern)\\b",
    // ... existing patterns
};
```

### Adding New Claim ID Patterns

Edit the `CLAIM_ID_PATTERNS` array:

```java
private static final String[] CLAIM_ID_PATTERNS = {
    "\\bYourPattern[\\s#:-]*([A-Z0-9]{6,15})\\b",
    // ... existing patterns
};
```

## Testing

Run the included test suite:

```bash
mvn test
```

Test with the provided sample file:

```bash
java -jar target/patient-info-extractor-1.0-SNAPSHOT-shaded.jar sample_medical_record.txt
```

## Dependencies

- **Stanford CoreNLP**: 4.5.0
- **Apache OpenNLP**: 1.9.4
- **JUnit**: 5.8.2 (testing)
- **SLF4J**: 1.7.32 (logging)

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## Support

For issues and questions:
- Create an issue in the repository
- Check the troubleshooting section
- Review the JavaDoc documentation

## Version History

- **v1.0.0**: Initial release with Stanford CoreNLP and Apache OpenNLP support
- Basic NER for patient names
- Regex-based date and claim ID extraction
- File processing capabilities
