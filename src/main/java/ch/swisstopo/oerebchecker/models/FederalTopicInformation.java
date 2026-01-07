package ch.swisstopo.oerebchecker.models;

import ch.swisstopo.oereb.V20Gesetze;
import ch.swisstopo.oereb.V20Themen;

import java.util.ArrayList;
import java.util.List;

public class FederalTopicInformation {

    private final V20Themen.DATASECTION.V20Thema.V20ThemaThema thema;
    private final List<V20Gesetze.DATASECTION.V20Dokumente.V20DokumenteDokument> dokumente;

    public FederalTopicInformation(V20Themen.DATASECTION.V20Thema.V20ThemaThema thema) {
        this.thema = thema;
        this.dokumente = new ArrayList<>();
    }

    public V20Themen.DATASECTION.V20Thema.V20ThemaThema getThema() {
        return thema;
    }

    public boolean addDokument(V20Gesetze.DATASECTION.V20Dokumente.V20DokumenteDokument dokument) {

        if (dokumente.contains(dokument)) {
            return false;
        }
        return dokumente.add(dokument);
    }

    public List<V20Gesetze.DATASECTION.V20Dokumente.V20DokumenteDokument> getDokumente() {
        return dokumente;
    }
}
