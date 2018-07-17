import it.uniroma1.lcl.babelnet.*;
import it.uniroma1.lcl.babelnet.data.BabelExample;
import it.uniroma1.lcl.babelnet.data.BabelGloss;
import it.uniroma1.lcl.babelnet.data.BabelPointer;
import it.uniroma1.lcl.jlt.util.Language;
import org.annolab.tt4j.TokenHandler;
import org.annolab.tt4j.TreeTaggerWrapper;
import java.io.*;
import java.util.*;

public class WSDBabelNet {
    static HashMap<String,String> wsdMap = new HashMap<String,String>();
    static BabelNet bn = BabelNet.getInstance();
    static String pathToStopWords = "[PATH_TO_STOP_WORDS_FILE]";
    static String pathToOutputFile = "[PATH_TO_LOG_OUTPUT_FILE]";
    public static void main(String[] args) {
        initInput();
        File log = new File(pathToOutputFile);
        int c = 1;
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(log)));
            Set<String> inputSentences = wsdMap.keySet();
            Iterator<String> it = inputSentences.iterator();
            while(it.hasNext()) {
                int maxOverlap = -1;
                BabelSense bestSense = null;
                System.out.println("Ciclo numero "+ (c++));
                String sentence = it.next();
                String word = wsdMap.get(sentence);
                Word lemmedWord = treeTag(word);
                String[] sentence_words = sentence.split(" ");
                ArrayList<String> array = new ArrayList<>();
                for (String s : sentence_words) {
                    array.add(s.toLowerCase());
                }
                ArrayList<Word> lemmedContext = treeTag(array);
                ArrayList<String> context = filterWords(lemmedContext);
                BabelNetQuery query = new BabelNetQuery.Builder(lemmedWord.getLemma())
                        .from(Language.IT)
                        .build();
                List<BabelSynset> byl = bn.getSynsets(query);
                for (int i = 0; i < byl.size(); i++) {
                    BabelSynset bsynExample = byl.get(i);
                    String inputWord = bsynExample.getMainSense(Language.IT).get().getFullLemma();
                    Optional<BabelGloss> inputWordGloss = bsynExample.getMainSense(Language.IT).get().getSynset().getMainGloss(Language.IT);
                    ArrayList<String> signature = new ArrayList<>();
                    if (inputWordGloss.isPresent()) {
                        ArrayList<String> splittedGloss = filterWords(inputWordGloss.get().getGloss());
                        ArrayList<Word> lemmedGloss = treeTag(splittedGloss);
                        signature.addAll(filterWords(lemmedGloss));
                    }
                    List<BabelExample> inputWordExamples = bsynExample.getExamples(Language.IT);
                    ArrayList<String> inputExamples = new ArrayList<>();
                    for (BabelExample example : inputWordExamples)
                        inputExamples.addAll(filterWords(example.getExample()));
                    signature.addAll(inputExamples);
                    ArrayList<BabelSynset> relatedSynsets = new ArrayList<>();
                    relatedSynsets.addAll(getRelatedSynsets(bsynExample, BabelPointer.ANY_HYPERNYM));
                    for (BabelSynset relSynset : relatedSynsets) {
                        Optional<BabelGloss> relTermGloss = relSynset.getMainGloss(Language.IT);
                        String gloss = "";
                        if (relTermGloss.isPresent()) {
                            gloss = relTermGloss.get().getGloss();
                            ArrayList<String> splittedGloss = filterWords(gloss);
                            ArrayList<Word> lemmedGloss = treeTag(splittedGloss);
                            signature.addAll(filterWords(lemmedGloss));
                        }
                        List<BabelExample> relTermExamples = relSynset.getExamples(Language.IT);
                        ArrayList<String> relExamples = new ArrayList<>();
                        for (BabelExample example : relTermExamples) {
                            relExamples.addAll(filterWords(example.getExample()));
                        }
                        signature.addAll(relExamples);
                    }
                    ArrayList<Word> lemmedSignature = treeTag(signature);
                    ArrayList<String> sig = filterWords(lemmedSignature);
                    int overlap = computeOverlap(context, sig);
                    if (overlap > maxOverlap) {
                        maxOverlap = overlap;
                        bestSense = bsynExample.getMainSense().get();
                    }
                }
                System.out.println("Stampa risultato "+ word + "...");
                writeOutput(word, sentence, bestSense, out);
            }
            out.close();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    private static void writeOutput(String word, String sentence, BabelSense bestSense, BufferedWriter out) throws IOException {
        try {
            out.write("\n==========");
            out.write("Parola: " + word + "\n");
            out.write("Frase: " + sentence + "\n");
            out.write("Miglior senso trovato: " + bestSense.getFullLemma() + "\n");
            if(bestSense != null && bestSense.getSynset() != null && bestSense.getSynset().getMainGloss(Language.IT)!= null) {
                if (bestSense.getSynset().getMainGloss(Language.IT).get() != null)
                    out.write("Definizione: " + bestSense.getSynset().getMainGloss(Language.IT).get() + "\n");
                else
                    out.write("ciao");
            }
            out.write("==========");
        }catch (Exception e){
            out.write(e.getMessage());
            e.printStackTrace();
        }
    }

    private static Word treeTag(String word){
        Word wor = new Word();
        System.setProperty("treetagger.home","[TREE_TAGGER_PATH]");
        TreeTaggerWrapper tt = new org.annolab.tt4j.TreeTaggerWrapper<String>();
        try{
            tt.setModel("italian-utf8.par");
            tt.setHandler(new TokenHandler<String>() {
                public void token(String token, String pos, String lemma) {
                    wor.setLessema(token);
                    wor.setLemma(lemma);
                    wor.setPos(pos);
                }
            });
            tt.process(Arrays.asList(word));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            tt.destroy();
            return wor;
        }
    }

    private static int computeOverlap(ArrayList<String> context, ArrayList<String> signature){
        int overlap = 0;
        for(String l1 : context){
            for(String l2 : signature){
                if(l1.equals(l2))
                    overlap++;
            }
        }
        return overlap;
    }

    private static ArrayList<Word> treeTag(ArrayList<String> sentenceWords){
        ArrayList<Word> lemmedWords = new ArrayList<Word>();
        System.setProperty("treetagger.home","[TREE_TAGGER_PATH]");
        TreeTaggerWrapper tt = new org.annolab.tt4j.TreeTaggerWrapper<String>();
        try{
            tt.setModel("italian-utf8.par");
            tt.setHandler(new TokenHandler<String>() {
                public void token(String token, String pos, String lemma) {
                    lemmedWords.add(new Word(token, lemma, pos));
                }
            });
            tt.process(sentenceWords);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            tt.destroy();
            return lemmedWords;
        }
    }

    private static List<String> getRelatedTerms(BabelSynset babelSynset, BabelPointer relationType){
        List<String> relatedTerms = new ArrayList<>();
        for(BabelSynsetRelation edge : babelSynset.getOutgoingEdges(relationType)){
            Optional<BabelSense> opt = edge.getBabelSynsetIDTarget().toSynset().getMainSense(Language.IT);
            String relTerm = "";
            if(opt.isPresent()) {
                relTerm = opt.get().getFullLemma();
                if(!relatedTerms.contains(relTerm))
                    relatedTerms.add(relTerm);
            }
        }
        return relatedTerms;
    }

    private static List<BabelSynset> getRelatedSynsets(BabelSynset babelSynset, BabelPointer relationType){
        List<BabelSynset> relatedTerms = new ArrayList<>();
        for(BabelSynsetRelation edge : babelSynset.getOutgoingEdges(relationType)){
            Optional<BabelSense> opt = edge.getBabelSynsetIDTarget().toSynset().getMainSense(Language.IT);
            BabelSynset relTerm;
            if(opt.isPresent()) {
                relTerm = opt.get().getSynset();
                if(!relatedTerms.contains(relTerm))
                    relatedTerms.add(relTerm);
            }
        }
        return relatedTerms;
    }


    private static ArrayList<String> filterWords(ArrayList<Word>lemmedSentence){
        ArrayList<String> lemmedSentenceWords = new ArrayList<>();
        try {
            ArrayList<String> filteredWords = new ArrayList<>();
            for(Word l : lemmedSentence) {
                String w = l.getLessema()
                        .replace(" ","")
                        .replace("'","")
                        .toLowerCase();
                Word filteredWord = l;
                filteredWord.setLessema(w);
                lemmedSentenceWords.add(filteredWord.getLemma());
                int index = lemmedSentenceWords.indexOf(filteredWord.getLemma());
                if(!(w.equals(".")) && !w.equals(";") && !w.equals(":") && !w.equals(",")) {
                    BufferedReader in = new BufferedReader(new FileReader(new File(pathToStopWords)));
                    String line = "";
                    while ((line = in.readLine()) != null) {
                        line = line.split(" ")[0];
                        line = line.replace(" ", "");
                        if (!line.equals("") && line.equals(w)) {
                            lemmedSentenceWords.remove(index);
                            in.mark(0);
                            in.reset();
                        }
                    }
                }else{
                    lemmedSentenceWords.remove(filteredWord.getLemma());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lemmedSentenceWords;
    }

    private static ArrayList<String> filterWords(String sentence){
        ArrayList<String> sentenceWords = new ArrayList<>();
        String[] words = sentence
                .replace(";"," ")
                .replace("'", "' ")
                .split(" ");
        for(String w : words) {
            w = w.replace(",", "")
                    .replace(";", "")
                    .replace(".", "")
                    .replace(":", "")
                    .replace(" ","")
                    .toLowerCase();
            sentenceWords.add(w);
        }
        try {
            BufferedReader in = new BufferedReader(new FileReader(new File(pathToStopWords)));
            String line = "";
            ArrayList<String> filteredWords = new ArrayList<>();
            while((line = in.readLine()) != null){
                line = line.split(" ")[0];
                if(!line.equals("") && sentenceWords.contains(line))
                    sentenceWords.remove(line);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sentenceWords;
    }


    private static void initInput(){
        wsdMap.put("La pianta dell' alloggio è disponibile in ufficio ,"
                + " accanto all' appartamento ; dal disegno è possibile cogliere i"
                + " dettagli dell' architettura dello stabile , sulla distribuzione"
                + " dei vani e la disposizione di porte e finestre", "pianta");
        wsdMap.put("I platani sono piante ad alto fusto , organismi viventi : non"
                + " ha senso toglierli per fare posto a un parcheggio", "piante");
        wsdMap.put("Non riesce ad appoggiare la pianta del piede perché ha un "
                + "profondo taglio vicino all’ alluce", "pianta");
        wsdMap.put("Si tratta di un uomo facilmente riconoscibile : ha una testa "
                + "piccola , gli occhi sporgenti , naso adunco e piccole orecchie a sventola", "testa");
        wsdMap.put("Come per tutte le cose , bisogna usare la testa , "
                + "una punta di cervello , per non prendere decisioni fuori dal senso dell’ intelletto", "testa");
        wsdMap.put("La testa della struttura di parsing è l’ elemento che "
                + "corrisponde al sintagma più alto dell’ albero ; in genere si tratta di un verbo", "testa");
        wsdMap.put("Amo andare a cavallo , ogni mattino , nel parco", "cavallo");
        wsdMap.put("Il cavallo di questi calzoni è troppo basso per la mia taglia", "cavallo");
        wsdMap.put("In palestra sono bravissima nel saltare il cavallo", "cavallo");
        wsdMap.put("La mia macchina ha 120 cavalli di potenza", "cavalli");
        wsdMap.put("Mi manca poco per concludere il lavoro : sono quasi a cavallo", "cavallo");
        wsdMap.put("Il fattore dirige la coltivazione dei campi per conto dei proprietari", "fattore");
        wsdMap.put("Il fattore principale del suo successo è la sua attitudine analitica", "fattore");
        wsdMap.put("I due fattori dell’ espressione devono soddisfare la proprietà di essere primi fra loro", "fattori");
        wsdMap.put("Il rombo è un pesce di mare ; porta entrambi gli occhi su di"
                + " un lato , nella fattispecie il sinistro , ed ha il lato privo"
                + " di occhi roseo e senza pigmenti ; ha la tipica forma quadrangolare", "rombo");
        wsdMap.put("Il rombo è una figura geometrica i cui quattro lati hanno la stessa lunghezza", "rombo");
        wsdMap.put("Ognuna delle 32 sezioni in cui si divide la rosa della bussola ,"
                + " e che corrispondono alle principali direzioni del vento , si dice rombo","rombo");
        wsdMap.put("Le vettovaglie caddero , producendo un rumore sordo , profondo e cupo, simile al rombo di un tuono", "rombo");
    }
}
