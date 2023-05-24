package com.company.util;


/**
 * implementation of binary search to find value in virtual nodes array
 */
public class BinarySearch {
    public static int search(Double[] virtualNodes, int start, int end, int value) {
        int mid = start + (end - start) / 2;
        if (mid == 0 || start >= end) {
            return mid;
        }
        if (virtualNodes[mid - 1] <= value && value < virtualNodes[mid]) {
            return mid - 1;
        }
        if (virtualNodes[mid] >= value) {
            return search(virtualNodes, start, mid, value);
        } else {
            return search(virtualNodes, mid + 1, end, value);
        }
    }
}
