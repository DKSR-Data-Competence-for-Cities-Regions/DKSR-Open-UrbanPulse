package city.ui.shared.commons.collections;


import java.util.Objects;

/**
 * creates Tuple object
 *
 * @param <T0> first part of the tuple
 * @param <T1> second part of the tuple
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class Tuple<T0, T1> {

    private T0 first;
    private T1 second;

    /**
     * Constructor
     *
     * @param first
     * @param second
     */
    public Tuple(T0 first, T1 second) {
        this.first = first;
        this.second = second;
    }

    public T0 getFirst() {
        return first;
    }

    public void setFirst(T0 first) {
        this.first = first;
    }

    public T1 getSecond() {
        return second;
    }

    public void setSecond(T1 second) {
        this.second = second;
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Tuple<?, ?> other = (Tuple<?, ?>) obj;
        return Objects.equals(this.first, other.first) && Objects.equals(this.second, other.second);
    }

    @Override
    public String toString() {
        return "Tuple{" + "first=" + first + ", second=" + second + '}';
    }
}
