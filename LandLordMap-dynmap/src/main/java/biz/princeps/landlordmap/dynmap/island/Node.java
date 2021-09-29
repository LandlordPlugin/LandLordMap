package biz.princeps.landlordmap.dynmap.island;

import java.util.Objects;

class Node {
    int x;
    int z;
    boolean visited;

    public Node(int x, int z) {
        this.x = x;
        this.z = z;
        this.visited = false;
    }

    public Node getLeft() {
        return new Node(x - 16, z);
    }

    public Node getRight() {
        return new Node(x + 16, z);
    }

    public Node getTop() {
        return new Node(x, z - 16);
    }

    public Node getBottom() {
        return new Node(x, z + 16);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Node node = (Node) o;
        return x == node.x &&
                z == node.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
        return "Node{" +
                "x=" + x +
                ", z=" + z +
                '}';
    }

}
