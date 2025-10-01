
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Simple Patient Information Extractor using Apache OpenNLP
 * This version uses only OpenNLP for NER and regex for dates and IDs
 * 
 * Required models:
 * - en-sent.bin (sentence detection)
 * - en-token.bin (tokenization)  
 * - en-ner-person.bin (person name extraction)
 * 
 * Download from: http://opennlp.sourceforge.net/models-1.5/
 */
public class OpenNLPPatientExtractor {

    private SentenceDetector sentenceDetector;
    private Tokenizer tokenizer;
    private NameFinderME personFinder;

    // Same regex patterns as the Stanford version
    private static final String[] DATE_PATTERNS = {
        "\\b(0[1-9]|1[0-2])[/-](0[1-9]|[12][0-9]|3[01])[/-](19|20)\\d{2}\\b",
        "\\b(0[1-9]|[12][0-9]|3[01])[/-](0[1-9]|1[0-2])[/-](19|20)\\d{2}\\b",
        "\\b(19|20)\\d{2}[/-](0[1-9]|1[0-2])[/-](0[1-9]|[12][0-9]|3[01])\\b",
        "\\b(January|February|March|April|May|June|July|August|September|October|November|December)\\s+(0[1-9]|[12][0-9]|3[01]),\\s+(19|20)\\d{2}\\b",
        "\\b(0[1-9]|[12][0-9]|3[01])\\s+(January|February|March|April|May|June|July|August|September|October|November|December)\\s+(19|20)\\d{2}\\b"
    };

    private static final String[] CLAIM_ID_PATTERNS = {
        "\\b[Cc]laim[\\s#:-]*([A-Z0-9]{6,15})\\b",
        "\\b[Cc]laim[\\s]*[Ii][Dd][\\s#:-]*([A-Z0-9]{6,15})\\b",
        "\\b[Cc]laim[\\s]*[Nn]umber[\\s#:-]*([A-Z0-9]{6,15})\\b",
        "\\bID[\\s#:-]*([A-Z0-9]{6,15})\\b",
        "\\b([A-Z]{2,4}[0-9]{6,12})\\b",
        "\\b([0-9]{8,15})\\b"
    };

    /**
     * Initialize OpenNLP models
     */
    public OpenNLPPatientExtractor() {
        try {
            // Initialize sentence detector
            InputStream sentenceModelStream = getClass().getResourceAsStream("/models/en-sent.bin");
            if (sentenceModelStream == null) {
                sentenceModelStream = new FileInputStream("models/en-sent.bin");
            }
            SentenceModel sentenceModel = new SentenceModel(sentenceModelStream);
            sentenceDetector = new SentenceDetectorME(sentenceModel);
            sentenceModelStream.close();

            // Initialize tokenizer
            InputStream tokenModelStream = getClass().getResourceAsStream("/models/en-token.bin");
            if (tokenModelStream == null) {
                tokenModelStream = new FileInputStream("models/en-token.bin");
            }
            TokenizerModel tokenModel = new TokenizerModel(tokenModelStream);
            tokenizer = new TokenizerME(tokenModel);
            tokenModelStream.close();

            // Initialize person name finder
            InputStream personModelStream = getClass().getResourceAsStream("/models/en-ner-person.bin");
            if (personModelStream == null) {
                personModelStream = new FileInputStream("models/en-ner-person.bin");
            }
            TokenNameFinderModel personModel = new TokenNameFinderModel(personModelStream);
            personFinder = new NameFinderME(personModel);
            personModelStream.close();

        } catch (IOException e) {
            System.err.println("Error loading OpenNLP models: " + e.getMessage());
            System.err.println("Please ensure OpenNLP model files are in the 'models' directory");
        }
    }

    /**
     * Extract patient information from file
     */
    public PatientInfo extractFromFile(String filePath) {
        try {
            String content = readFile(filePath);
            return extractPatientInfo(content);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return new PatientInfo();
        }
    }

    /**
     * Extract patient information from text
     */
    public PatientInfo extractPatientInfo(String text) {
        PatientInfo info = new PatientInfo();

        if (sentenceDetector != null && tokenizer != null && personFinder != null) {
            info.setPatientNames(extractNamesWithOpenNLP(text));
        } else {
            info.setPatientNames(extractNamesWithRegex(text));
        }

        info.setDatesOfBirth(extractDates(text));
        info.setClaimIds(extractClaimIds(text));

        return info;
    }

    /**
     * Extract names using OpenNLP
     */
    private List<String> extractNamesWithOpenNLP(String text) {
        List<String> names = new ArrayList<>();

        try {
            // Detect sentences
            String[] sentences = sentenceDetector.sentDetect(text);

            for (String sentence : sentences) {
                // Tokenize
                String[] tokens = tokenizer.tokenize(sentence);

                // Find person names
                Span[] nameSpans = personFinder.find(tokens);

                for (Span span : nameSpans) {
                    StringBuilder name = new StringBuilder();
                    for (int i = span.getStart(); i < span.getEnd(); i++) {
                        if (name.length() > 0) name.append(" ");
                        name.append(tokens[i]);
                    }

                    String fullName = name.toString();
                    if (fullName.length() > 1 && !names.contains(fullName)) {
                        names.add(fullName);
                    }
                }
            }

            // Clear adaptive data
            personFinder.clearAdaptiveData();

        } catch (Exception e) {
            System.err.println("Error in OpenNLP name extraction: " + e.getMessage());
            return extractNamesWithRegex(text);
        }

        return names;
    }

    /**
     * Fallback regex-based name extraction
     */
    private List<String> extractNamesWithRegex(String text) {
        List<String> names = new ArrayList<>();
        String[] patterns = {
            "\\bPatient[\\s:]+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)",
            "\\bName[\\s:]+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)",
            "\\b(?:Mr|Mrs|Ms|Dr)\\.?\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)"
        };

        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(text);
            while (matcher.find()) {
                String name = matcher.group(1).trim();
                if (!names.contains(name)) {
                    names.add(name);
                }
            }
        }

        return names;
    }

    /**
     * Extract dates with context awareness
     */
    private List<String> extractDates(String text) {
        List<String> dates = new ArrayList<>();

        // First try to find birth-related dates
        String[] birthPatterns = {
            "\\b(?:birth\\s*date|date\\s*of\\s*birth|born|DOB)[\\s:]*([^\\n.;]{1,30})",
        };

        for (String birthPattern : birthPatterns) {
            Matcher matcher = Pattern.compile(birthPattern, Pattern.CASE_INSENSITIVE).matcher(text);
            while (matcher.find()) {
                String context = matcher.group(1);
                dates.addAll(extractDatesFromText(context));
            }
        }

        // If no birth dates found, extract all dates
        if (dates.isEmpty()) {
            dates.addAll(extractDatesFromText(text));
        }

        return dates;
    }

    private List<String> extractDatesFromText(String text) {
        List<String> dates = new ArrayList<>();

        for (String pattern : DATE_PATTERNS) {
            Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text);
            while (matcher.find()) {
                String date = matcher.group().trim();
                if (!dates.contains(date)) {
                    dates.add(date);
                }
            }
        }

        return dates;
    }

    /**
     * Extract claim IDs
     */
    private List<String> extractClaimIds(String text) {
        List<String> claimIds = new ArrayList<>();

        for (String pattern : CLAIM_ID_PATTERNS) {
            Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text);
            while (matcher.find()) {
                String claimId = matcher.groupCount() > 0 ? matcher.group(1) : matcher.group();
                claimId = claimId.trim();

                if (claimId.length() >= 6 && !claimIds.contains(claimId)) {
                    claimIds.add(claimId);
                }
            }
        }

        return claimIds;
    }

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

    public static void main(String[] args) {
        OpenNLPPatientExtractor extractor = new OpenNLPPatientExtractor();

        if (args.length > 0) {
            PatientInfo info = extractor.extractFromFile(args[0]);
            System.out.println(info);
        } else {
            // Demo with sample text
            String sampleText = """
                Medical Report
                ===============
                Patient: Dr. Emily Johnson
                Date of Birth: March 15, 1978
                Claim Number: MED2024001234

                Patient Mr. Robert Smith (DOB: 12/05/1965) visited for consultation.
                Claim ID: ABC987654321

                Additional Information:
                - Patient Name: Maria Garcia  
                - Born: January 8, 1990
                - Medical ID: XYZ123456789
                """;

            PatientInfo info = extractor.extractPatientInfo(sampleText);
            System.out.println(info);
        }
    }
}
