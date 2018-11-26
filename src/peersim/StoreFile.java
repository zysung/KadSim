package peersim;

import java.math.BigInteger;

public class StoreFile {

    private BigInteger key;
    private Object value;
    private int size = 64;
    private int storeNodeRemainSize;

    public StoreFile(BigInteger key, Object value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "StoreFile{" +
                "key=" + key +
                ", value=" + value +
                ", size=" + size +
                ", storeNodeRemainSize=" + storeNodeRemainSize +
                '}';
    }

    public BigInteger getKey() {
        return key;
    }

    public void setKey(BigInteger key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getStoreNodeRemainSize() {
        return storeNodeRemainSize;
    }

    public void setStoreNodeRemainSize(int storeNodeRemainSize) {
        this.storeNodeRemainSize = storeNodeRemainSize;
    }
}
