
import edu.stanford.nlp.ner.CRFClassifier;
import edu.stanford.nlp.ner.AbstractSequenceClassifier;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.ner.NERClassifierCombiner;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * PatientInformationExtractor - Extracts patient name, date of birth, and claim ID 
 * from medical text files using Java NLP tools and regex patterns
 * 
 * Requirements:
 * - Stanford CoreNLP library
 * - Stanford NER models (english.all.3class.distsim.crf.ser.gz)
 * 
 * @author Medical Text Processing System
 */
public class PatientInformationExtractor {

    private StanfordCoreNLP pipeline;
    private AbstractSequenceClassifier<CoreLabel> classifier;

    // Regex patterns for various date formats
    private static final String[] DATE_PATTERNS = {
        // MM/DD/YYYY or MM-DD-YYYY
        "\\b(0[1-9]|1[0-2])[/-](0[1-9]|[12][0-9]|3[01])[/-](19|20)\\d{2}\\b",
        // DD/MM/YYYY or DD-MM-YYYY  
        "\\b(0[1-9]|[12][0-9]|3[01])[/-](0[1-9]|1[0-2])[/-](19|20)\\d{2}\\b",
        // YYYY-MM-DD
        "\\b(19|20)\\d{2}[/-](0[1-9]|1[0-2])[/-](0[1-9]|[12][0-9]|3[01])\\b",
        // Month DD, YYYY (e.g., January 15, 1985)
        "\\b(January|February|March|April|May|June|July|August|September|October|November|December)\\s+(0[1-9]|[12][0-9]|3[01]),\\s+(19|20)\\d{2}\\b",
        // DD Month YYYY (e.g., 15 January 1985)
        "\\b(0[1-9]|[12][0-9]|3[01])\\s+(January|February|March|April|May|June|July|August|September|October|November|December)\\s+(19|20)\\d{2}\\b"
    };

    // Regex patterns for claim IDs
    private static final String[] CLAIM_ID_PATTERNS = {
        // Standard claim ID formats
        "\\b[Cc]laim[\\s#:-]*([A-Z0-9]{6,15})\\b",
        "\\b[Cc]laim[\\s]*[Ii][Dd][\\s#:-]*([A-Z0-9]{6,15})\\b",
        "\\b[Cc]laim[\\s]*[Nn]umber[\\s#:-]*([A-Z0-9]{6,15})\\b",
        "\\b[Cc]laim[\\s]*[Nn]o[\\s#:-]*([A-Z0-9]{6,15})\\b",
        "\\bID[\\s#:-]*([A-Z0-9]{6,15})\\b",
        "\\b([A-Z]{2,4}[0-9]{6,12})\\b", // Generic alphanumeric ID
        "\\b([0-9]{8,15})\\b" // Pure numeric ID (8-15 digits)
    };

    /**
     * Constructor - Initialize the NLP pipeline and NER classifier
     */
    public PatientInformationExtractor() {
        try {
            // Initialize Stanford CoreNLP pipeline
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
            props.setProperty("ner.useSUTime", "false");
            this.pipeline = new StanfordCoreNLP(props);

            // Initialize NER classifier for person names
            String serializedClassifier = "english.all.3class.distsim.crf.ser.gz";
            this.classifier = CRFClassifier.getClassifier(serializedClassifier);

        } catch (Exception e) {
            System.err.println("Error initializing NLP tools: " + e.getMessage());
            // Fallback: use regex-based extraction only
            this.pipeline = null;
            this.classifier = null;
        }
    }

    /**
     * Extract patient information from a text file
     * @param filePath Path to the text file containing patient information
     * @return PatientInfo object containing extracted information
     */
    public PatientInfo extractPatientInfo(String filePath) {
        try {
            String content = readFile(filePath);
            return extractPatientInfo(content);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return new PatientInfo();
        }
    }

    /**
     * Extract patient information from text content
     * @param content Text content containing patient information
     * @return PatientInfo object containing extracted information
     */
    public PatientInfo extractPatientInfo(String content) {
        PatientInfo patientInfo = new PatientInfo();

        // Extract patient names using NER
        patientInfo.setPatientNames(extractPatientNames(content));

        // Extract dates of birth using regex patterns
        patientInfo.setDatesOfBirth(extractDatesOfBirth(content));

        // Extract claim IDs using regex patterns
        patientInfo.setClaimIds(extractClaimIds(content));

        return patientInfo;
    }

    /**
     * Extract patient names using Stanford NER
     * @param content Text content
     * @return List of detected patient names
     */
    private List<String> extractPatientNames(String content) {
        List<String> names = new ArrayList<>();

        try {
            if (pipeline != null) {
                // Use Stanford CoreNLP for NER
                Annotation document = new Annotation(content);
                pipeline.annotate(document);

                List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
                for (CoreMap sentence : sentences) {
                    List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

                    StringBuilder currentName = new StringBuilder();
                    for (CoreLabel token : tokens) {
                        String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                        String word = token.get(CoreAnnotations.TextAnnotation.class);

                        if ("PERSON".equals(ne)) {
                            if (currentName.length() > 0) {
                                currentName.append(" ");
                            }
                            currentName.append(word);
                        } else {
                            if (currentName.length() > 0) {
                                String fullName = currentName.toString().trim();
                                if (fullName.length() > 1 && !names.contains(fullName)) {
                                    names.add(fullName);
                                }
                                currentName = new StringBuilder();
                            }
                        }
                    }

                    // Add any remaining name
                    if (currentName.length() > 0) {
                        String fullName = currentName.toString().trim();
                        if (fullName.length() > 1 && !names.contains(fullName)) {
                            names.add(fullName);
                        }
                    }
                }
            } else if (classifier != null) {
                // Use CRF classifier as fallback
                String classifiedText = classifier.classifyToString(content);
                // Parse the classified text to extract PERSON entities
                String[] lines = classifiedText.split("\\n");
                StringBuilder currentName = new StringBuilder();

                for (String line : lines) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        String word = parts[0];
                        String tag = parts[parts.length - 1];

                        if ("PERSON".equals(tag)) {
                            if (currentName.length() > 0) {
                                currentName.append(" ");
                            }
                            currentName.append(word);
                        } else {
                            if (currentName.length() > 0) {
                                String fullName = currentName.toString().trim();
                                if (fullName.length() > 1 && !names.contains(fullName)) {
                                    names.add(fullName);
                                }
                                currentName = new StringBuilder();
                            }
                        }
                    }
                }

                if (currentName.length() > 0) {
                    String fullName = currentName.toString().trim();
                    if (fullName.length() > 1 && !names.contains(fullName)) {
                        names.add(fullName);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in name extraction: " + e.getMessage());
        }

        // If NLP fails, use regex patterns as fallback for common name patterns
        if (names.isEmpty()) {
            names.addAll(extractNamesWithRegex(content));
        }

        return names;
    }

    /**
     * Fallback method to extract names using regex patterns
     * @param content Text content
     * @return List of potential names
     */
    private List<String> extractNamesWithRegex(String content) {
        List<String> names = new ArrayList<>();

        // Common patterns for names in medical documents
        String[] namePatterns = {
            "\\bPatient[\\s:]+([A-Z][a-z]+\\s+[A-Z][a-z]+)",
            "\\bName[\\s:]+([A-Z][a-z]+\\s+[A-Z][a-z]+)",
            "\\bMr\\.?\\s+([A-Z][a-z]+\\s+[A-Z][a-z]+)",
            "\\bMrs\\.?\\s+([A-Z][a-z]+\\s+[A-Z][a-z]+)",
            "\\bMs\\.?\\s+([A-Z][a-z]+\\s+[A-Z][a-z]+)",
            "\\bDr\\.?\\s+([A-Z][a-z]+\\s+[A-Z][a-z]+)"
        };

        for (String pattern : namePatterns) {
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(content);
            while (m.find()) {
                String name = m.group(1).trim();
                if (name.length() > 1 && !names.contains(name)) {
                    names.add(name);
                }
            }
        }

        return names;
    }

    /**
     * Extract dates of birth using regex patterns
     * @param content Text content
     * @return List of detected dates
     */
    private List<String> extractDatesOfBirth(String content) {
        List<String> dates = new ArrayList<>();

        // Look for dates in context of birth-related keywords
        String[] birthContextPatterns = {
            "\\b[Bb]irth[\\s]*[Dd]ate[\\s:]*([^\\n]*)",
            "\\b[Dd]ate[\\s]*of[\\s]*[Bb]irth[\\s:]*([^\\n]*)",
            "\\b[Bb]orn[\\s]*[on]*[\\s:]*([^\\n]*)",
            "\\bDOB[\\s:]*([^\\n]*)"
        };

        for (String contextPattern : birthContextPatterns) {
            Pattern p = Pattern.compile(contextPattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(content);
            while (m.find()) {
                String contextText = m.group(1);
                // Extract dates from the context text
                for (String datePattern : DATE_PATTERNS) {
                    Pattern dateP = Pattern.compile(datePattern, Pattern.CASE_INSENSITIVE);
                    Matcher dateM = dateP.matcher(contextText);
                    while (dateM.find()) {
                        String date = dateM.group().trim();
                        if (!dates.contains(date)) {
                            dates.add(date);
                        }
                    }
                }
            }
        }

        // If no birth-specific dates found, extract all dates and let user filter
        if (dates.isEmpty()) {
            for (String datePattern : DATE_PATTERNS) {
                Pattern p = Pattern.compile(datePattern, Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(content);
                while (m.find()) {
                    String date = m.group().trim();
                    if (!dates.contains(date)) {
                        dates.add(date);
                    }
                }
            }
        }

        return dates;
    }

    /**
     * Extract claim IDs using regex patterns
     * @param content Text content
     * @return List of detected claim IDs
     */
    private List<String> extractClaimIds(String content) {
        List<String> claimIds = new ArrayList<>();

        for (String pattern : CLAIM_ID_PATTERNS) {
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(content);
            while (m.find()) {
                String claimId;
                if (m.groupCount() > 0) {
                    claimId = m.group(1).trim();
                } else {
                    claimId = m.group().trim();
                }

                if (claimId.length() >= 6 && !claimIds.contains(claimId)) {
                    claimIds.add(claimId);
                }
            }
        }

        return claimIds;
    }

    /**
     * Read content from a text file
     * @param filePath Path to the file
     * @return File content as string
     * @throws IOException if file cannot be read
     */
    private String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * Main method to demonstrate usage
     */
    public static void main(String[] args) {
        PatientInformationExtractor extractor = new PatientInformationExtractor();

        // Example usage with a file
        if (args.length > 0) {
            String filePath = args[0];
            PatientInfo info = extractor.extractPatientInfo(filePath);
            System.out.println(info);
        } else {
            // Example with sample text
            String sampleText = """
                MEDICAL RECORD

                Patient Name: John Smith
                Date of Birth: January 15, 1985
                Claim ID: CLM123456789

                Patient Information:
                Mr. John Smith was born on 01/15/1985 and has been experiencing symptoms.
                This claim (Claim Number: ABC987654321) is related to his recent medical visit.

                Additional patient: Sarah Johnson
                DOB: 03/22/1992
                Claim No: DEF555444333

                Treatment provided to Ms. Sarah Johnson born March 22, 1992.
                Medical ID: 1234567890123
                """;

            PatientInfo info = extractor.extractPatientInfo(sampleText);
            System.out.println(info);
        }
    }
}

/**
 * Data class to hold extracted patient information
 */
class PatientInfo {
    private List<String> patientNames = new ArrayList<>();
    private List<String> datesOfBirth = new ArrayList<>();
    private List<String> claimIds = new ArrayList<>();

    // Getters and setters
    public List<String> getPatientNames() {
        return patientNames;
    }

    public void setPatientNames(List<String> patientNames) {
        this.patientNames = patientNames;
    }

    public List<String> getDatesOfBirth() {
        return datesOfBirth;
    }

    public void setDatesOfBirth(List<String> datesOfBirth) {
        this.datesOfBirth = datesOfBirth;
    }

    public List<String> getClaimIds() {
        return claimIds;
    }

    public void setClaimIds(List<String> claimIds) {
        this.claimIds = claimIds;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== EXTRACTED PATIENT INFORMATION ===\n");

        sb.append("\nPatient Names:");
        if (patientNames.isEmpty()) {
            sb.append(" None found");
        } else {
            for (String name : patientNames) {
                sb.append("\n  - ").append(name);
            }
        }

        sb.append("\n\nDates of Birth:");
        if (datesOfBirth.isEmpty()) {
            sb.append(" None found");
        } else {
            for (String date : datesOfBirth) {
                sb.append("\n  - ").append(date);
            }
        }

        sb.append("\n\nClaim IDs:");
        if (claimIds.isEmpty()) {
            sb.append(" None found");
        } else {
            for (String claimId : claimIds) {
                sb.append("\n  - ").append(claimId);
            }
        }

        sb.append("\n\n=====================================");

        return sb.toString();
    }
}
