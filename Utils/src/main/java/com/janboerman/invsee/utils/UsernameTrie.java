package com.janboerman.invsee.utils;

import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * <p>
 *  This class represents a Trie, specialised for minecraft usernames.
 * </p>
 * <p>
 *  A Trie&ltV&gt is a datastructure much like Map&ltK, V&gt, but optimised for string keys.
 * </p>
 * <p>
 *  This Trie implementation does not support {@code null} keys.
 * </p>
 * @param <V> the value type
 */
public class UsernameTrie<V> {

    //TODO we might want to be case in-sensitive

    private final Node<V> root;

    /**
     * Constructs a UsernameTrie with value {@code rootValue} for the empty string key.
     * @param rootValue the value for the empty key
     */
    public UsernameTrie(V rootValue) {
        this.root = new Node<>(new char[0], Maybe.just(rootValue), null);
    }

    public UsernameTrie() {
        this.root = new Node<>(new char[0], Maybe.nothing(), null);
    }

    public Maybe<V> insert(String username, V value) {
        return insert(username.toCharArray(), value);
    }

    public Maybe<V> insert(char[] username, V value) {
        if (!Username.isValidUsername(username))
            return Maybe.nothing();

        Node<V> node = root.lookup(username);
        Maybe<V> oldValue = node.value;
        node.value = Maybe.just(value);
        return oldValue;
    }

    public Maybe<V> delete(String username) {
        return delete(username.toCharArray());
    }

    public Maybe<V> delete(char[] username) {
        if (!Username.isValidUsername(username))
            return Maybe.nothing();

        Node<V> node = root.lookup(username);
        Maybe<V> oldValue = node.value;
        node.value = Maybe.nothing();
        node.cleanUp();
        return oldValue;
    }

    public Maybe<V> get(String username) {
        return get(username.toCharArray());
    }

    public Maybe<V> get(char[] username) {
        if (!Username.isValidUsername(username))
            return Maybe.nothing();

        Node<V> node = root.lookup(username);
        Maybe<V> value = node.value;
        node.cleanUp();
        return value;
    }

    public void traverse(String prefix, BiConsumer<String, ? super V> consumer) {
        traverse(prefix.toCharArray(), (chars, v) -> consumer.accept(new String(chars), v));
    }

    public void traverse(char[] prefix, BiConsumer<char[], ? super V> consumer) {
        if (prefix.length > 16 || !Username.isValidCharacters(prefix))
            return;

        Node<V> node = root.lookup(prefix);
        node.traverse(consumer);
        node.cleanUp();
    }

    private static class Node<V> {

        /*not-null, except for the root node*/
        private final char[] segment; //TODO don't we want byte[] instead?
        /*nullable*/
        private Maybe<V> value;

        /*nullable*/
        private Node<V>[] children;
        /*not-null, except for the root node*/
        private Node<V> parent;

        private Node(char[] segment, Maybe<V> value, Node<V> parent) {
            this(segment, value, null, parent);
        }

        private Node(char[] segment, Maybe<V> value, Node<V>[] children, Node<V> parent) {
            assert segment != null : "segment cannot be null";
            assert value != null : "value cannot be null";

            this.segment = segment;
            this.value = value;
            this.children = children;
            this.parent = parent;
        }

        private boolean isEmpty() {
            // do we have a value?
            if (value.isPresent())
                return false;
            // are all our children empty?
            if (children == null)
                return false;
            for (Node<V> child : children)
                if (child != null && !child.isEmpty())
                    return false;
            // if the answers to both questions are 'yes', then we are empty!
            return true;
        }

        private void cleanUp() {
            if (parent != null) {
                if (isEmpty()) {
                    //we are empty
                    //remove `this` from parent:

                    parent.children[Username.toIndex(segment[0])] = null;
                    parent.cleanUp();
                } else if (!value.isPresent()) {
                    //we have no value
                    //if we are the single node between to others, make our child a direct child of our parent:

                    for (int i = 0; i < parent.children.length; i++) {
                        if (i != Username.toIndex(segment[0]) && parent.children[i] != null) return; //we are in fact not the only child.
                    }
                    if (children == null) return; //we don't in fact have any children

                    int childCount = 0;
                    int idx = -1;
                    for (int i = 0; i < children.length; i++) {
                        if (children[i] != null) {
                            childCount += 1;
                            idx = i;
                        }
                        if (childCount > 1) return; //we have more than one child.
                    }
                    assert 0 <= idx && idx < children.length;
                    //we are the only child, concat our segment and our child's segment.

                    final char[] newSegment = ArrayHelper.concat(segment, children[idx].segment);
                    final Node<V> longNode = new Node<>(newSegment, children[idx].value, children[idx].children, parent);
                    parent.children[Username.toIndex(newSegment[0])] = longNode;
                    parent.cleanUp();
                }
                //else: we do have a value, so don't perform any clean-up.
            }
            //else: there is no parent, we are the root!
        }

        private void ensureChildrenArray() {
            if (children == null) children = new Node[Username.lookupTableSize()];
        }

        private Node<V> lookup(final char[] segment) {
            assert segment != null : "lookup segment cannot be null";

            if (segment.length == 0) return this;

            ensureChildrenArray();
            final int atIndex = Username.toIndex(segment[0]);
            Node<V> child = children[atIndex];
            if (child == null) {
                //the child does not exist!
                //create a new one and return it!
                child = new Node<>(segment, Maybe.nothing(), this);
                this.children[atIndex] = child;
                return child;
            } else {
                //the child does exist!
                //we need to find the common prefix

                final char[] childSegment = child.segment;
                int i;
                for (i = 0; i < childSegment.length && i < segment.length && childSegment[i] == segment[i]; i += 1);

                //i is the index at which they differ.
                //if i is equal to the lowest length of the two segments, then the segements didn't differ!

                if (i == Math.min(childSegment.length, segment.length)) {
                    //case distinction:
                    //  |childSegment| == |segment| ==> replace value!
                    //  |childSegment| > |segment| ==> 'segment' becomes the new child of `this`, and 'childSegment' gets its tail as a child of 'segment'
                    //  |childSegment| < |segment| ==> suffix of 'segment' becomes a child of 'childSegment'.

                    if (childSegment.length == segment.length) {
                        //the segments are the same!
                        return child;
                    } else if (childSegment.length < segment.length) {
                        // lookup the suffix of 'segment' for the child
                        char[] suffix = Arrays.copyOfRange(segment, childSegment.length, segment.length);
                        return child.lookup(suffix);
                    } else {
                        assert childSegment.length > segment.length;
                        // insert 'segment' in between
                        char[] suffix = Arrays.copyOfRange(childSegment, segment.length, childSegment.length);
                        Node<V> replacingChild = new Node<>(segment, Maybe.nothing(), this);
                        Node<V> grandChild = new Node<>(suffix, child.value, child.children, replacingChild);
                        replacingChild.ensureChildrenArray();
                        replacingChild.children[Username.toIndex(suffix[0])] = grandChild;
                        this.children[atIndex] = replacingChild;

                        return replacingChild;
                    }
                } else {
                    // childSegment and segment have a common prefix, and one does not subsume the other!

                    // we need to split up childSegment to the common prefix
                    // then, create three nodes:
                    //  - replacingChild which replaces the current child.
                    //  - grandChild1 which becomes a child of replacingChild (suffix from childSegment)
                    //  - grandChild2 which becomes a child of replacingChild (suffix from segment)
                    final char[] commonPrefix = Arrays.copyOfRange(childSegment, 0, i);
                    final char[] suffixChildSegment = Arrays.copyOfRange(childSegment, i, childSegment.length);
                    final char[] suffixSegment = Arrays.copyOfRange(segment, i, segment.length);

                    final Node<V> replacingChild = new Node<>(commonPrefix, Maybe.nothing(), new Node[Username.lookupTableSize()] /*fill later*/, this);
                    final Node<V> grandChild1 = new Node<>(suffixChildSegment, child.value, child.children, replacingChild);
                    final Node<V> grandChild2 = new Node<>(suffixSegment, Maybe.nothing(), null, replacingChild);
                    replacingChild.children[Username.toIndex(suffixChildSegment[0])] = grandChild1;
                    replacingChild.children[Username.toIndex(suffixSegment[0])] = grandChild2;

                    this.children[atIndex] = replacingChild;
                    return grandChild2;
                }
            }
        }

        private int length() {
            return (parent == null ? 0 : parent.length()) + segment.length;
        }

        private char[] fullString() {
            int last = length();
            final char[] result = new char[last];
            Node<V> node = this;
            while (node != null) {
                final char[] segment = node.segment;
                final int segmentLength = segment.length;
                System.arraycopy(segment, 0, result, last - segmentLength, segmentLength);
                last -= segmentLength;
                node = node.parent;
            }
            return result;
        }

        private void traverse(BiConsumer<char[], ? super V> consumer) {
            //when we get here - we are guaranteed that 'this' is a node

            //only accept nodes whose value are Just(something)
            if (value.isPresent()) {
                consumer.accept(fullString(), value.get());
            }

            if (children != null) {
                for (Node<V> child : children) {
                    if (child != null) {
                        child.traverse(consumer);
                    }
                }
            }
        }
    }

}

