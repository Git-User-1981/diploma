package searchengine.services.morphology;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class MorphologyAnalyzer {
    private static volatile MorphologyAnalyzer instance;
    private final LuceneMorphology russianMorph;
    private final LuceneMorphology englishMorph;
    private static final String[] PARTICLES = new String[]{
            "СОЮЗ",
            "МЕЖД",
            "ПРЕДЛ",
            "ЧАСТ",
            "ВВОДН",
            "PN",
            "PREP",
            "PART",
            "ARTICLE",
            "МС",
            "CONJ",
            "ADJECTIVE"
    };

    private MorphologyAnalyzer() throws IOException {
        russianMorph = new RussianLuceneMorphology();
        englishMorph = new EnglishLuceneMorphology();
    }

    public static MorphologyAnalyzer getInstance() throws IOException {
        MorphologyAnalyzer localInstance = instance;
        if (instance == null) {
            synchronized (MorphologyAnalyzer.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new MorphologyAnalyzer();
                }
            }
        }
        return localInstance;
    }

    private LuceneMorphology getMorphologyByWord(String word) {
        if (word.matches("[а-яё-]*")) {
            return russianMorph;
        }
        else if (word.matches("[a-z-]*")) {
            return englishMorph;
        }
        return null;
    }

    private boolean isParticle(String wordBase) {
        for (String particle : PARTICLES) {
            if (wordBase.toUpperCase().contains(particle)) {
                return true;
            }
        }
        return false;
    }

    public List<String> wordToLemma(String word) {
        if (word.isBlank()) {
            return null;
        }

        LuceneMorphology luceneMorphology = getMorphologyByWord(word);

        if (luceneMorphology == null) {
            return null;
        }

        List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
        if (wordBaseForms.stream().anyMatch(this::isParticle)) {
            return null;
        }

        List<String> normalForms = luceneMorphology.getNormalForms(word);
        if (normalForms.isEmpty()) {
            return null;
        }

        return normalForms;
    }

    public HashMap<String, Integer> getLemmaListWithCount(String text) {
        final HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : textToWordsArray(text)) {
            List<String> normalWords = wordToLemma(word);
            if (normalWords != null) {
                normalWords.forEach(nm -> lemmas.put(nm, lemmas.getOrDefault(nm, 0) + 1));
            }
        }

        return lemmas;
    }

    public List<String> getLemmaList(String text) {
        final List<String> lemmas = new ArrayList<>();

        for (String word : textToWordsArray(text)) {
            List<String> normalWords = wordToLemma(word);
            if (normalWords != null) {
                normalWords.forEach(normalWord -> {
                    if (normalWord != null && !lemmas.contains(normalWord)) {
                        lemmas.add(normalWord);
                    }
                });
            }
        }

        return lemmas;
    }

    public String[] textToWordsArray(String text) {
        return text.toLowerCase().replaceAll("[\\p{Punct}«»—:]+", "").trim().split("\\s+");
    }
}
