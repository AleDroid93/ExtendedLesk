public class Word {
    private String lessema;
    private String lemma;
    private String pos;

    public Word(String lessema, String lemma, String pos) {
        this.lessema = lessema;
        this.lemma = lemma;
        this.pos = pos;
    }

    public Word() {
        this.lessema = "";
        this.lemma = "";
        this.pos = "";
    }

    public String getLessema() {
        return lessema;
    }

    public void setLessema(String lessema) {
        this.lessema = lessema;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    @Override
    public String toString() {
        return "Word{" +
                "lessema='" + lessema + '\'' +
                ", lemma='" + lemma + '\'' +
                ", pos='" + pos + '\'' +
                '}';
    }
}
