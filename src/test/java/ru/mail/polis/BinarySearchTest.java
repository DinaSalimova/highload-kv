package ru.mail.polis;

import org.junit.Assert;
import org.junit.Test;
import com.company.util.BinarySearch;

public class BinarySearchTest {

    @Test
    public void binarySearchFindLeft() {
        Double[] virtualNodes = {1., 2., 3., 4., 5., 6.};
        int value = 1;
        int result = BinarySearch.search(virtualNodes, 0, virtualNodes.length - 1, value);
        Assert.assertEquals(1., virtualNodes[result], 0);
    }

    @Test
    public void binarySearchFindRight() {
        Double[] virtualNodes = {1., 2., 3., 4., 5., 6.};
        int value = 6;
        int result = BinarySearch.search(virtualNodes, 0, virtualNodes.length - 1, value);
        Assert.assertEquals(6., virtualNodes[result], 0);
    }

    @Test
    public void binarySearchFindMiddle() {
        Double[] virtualNodes = {0., 3.1, 3.2, 3.3, 3.4, 3.5, 3.6};
        int value = 3;
        int result = BinarySearch.search(virtualNodes, 0, virtualNodes.length - 1, value);
        Assert.assertEquals(0., virtualNodes[result], 0);
    }
}
