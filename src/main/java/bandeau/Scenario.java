package bandeau;

import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Classe utilitaire pour représenter la classe-association UML
 */
class ScenarioElement {

    Effect effect;
    int repeats;

    ScenarioElement(Effect e, int r) {
        effect = e;
        repeats = r;
    }
}

/**
 * Un scenario mémorise une liste d'effets, et le nombre de repetitions pour chaque effet
 * Un scenario sait se jouer sur un bandeau.
 */
public class Scenario {

    private final List<ScenarioElement> myElements = new LinkedList<>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Object bandeauLock = new Object(); // 🔒 Empêche l'exécution concurrente sur un même bandeau

    /**
     * Ajouter un effect au scenario.
     *
     * @param e       l'effet à ajouter
     * @param repeats le nombre de répétitions pour cet effet
     */
    public void addEffect(Effect e, int repeats) {
        lock.writeLock().lock(); // 🔒 Empêche l'ajout d'effets pendant l'exécution
        try {
            myElements.add(new ScenarioElement(effect, repeats));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Jouer ce scenario sur un bandeau
     *
     * @param b le bandeau ou s'afficher.
     */
    public void playOn(BandeauVerrouillable b) {
        new Thread(() -> {
            b.verrouille();
            try {

                play(b);

            } finally {
                b.deverrouille();
            }
        }).start();

    }

    public Future<Integer> play(Bandeau b) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        return executor.submit(() -> {
            int totalEffects = 0;

            synchronized (bandeauLock) { // 🔒 Vérifie que le bandeau n'est pas déjà utilisé
                lock.readLock().lock(); // 🔒 Empêche la modification de la liste des effets pendant l'exécution
                try {
                    for (ScenarioElement element : myElements) {
                        for (int repeats = 0; repeats < element.repeats; repeats++) {
                            element.effect.playOn(b);
                            totalEffects++;
                        }
                    }
                } finally {
                    lock.readLock().unlock();
                }
            }

            executor.shutdown(); // 🔚 Ferme l'executor après l'exécution
            return totalEffects;
        });
    }

}


