//! The stable sort Kotlin's `sortedWith` runs on the JVM, for comparators that are not total
//! orders.
//!
//! `assign_places` sorts the series rows with a comparator whose tiebreak (GSI 14.5 / D5) skips
//! the races where *either* boat was the RC volunteer, so it is not transitive in general: the
//! race it skips depends on the pair. Rust's `slice::sort_by` may panic on such a comparator;
//! `java.util.Arrays.sort(T[], Comparator)` never does, and for fewer than 32 elements it runs
//! `countRunAndMakeAscending` followed by a binary insertion sort (`TimSort.binarySort`). This is
//! that algorithm, so the Rust standings place the rows exactly as the Kotlin server did, even
//! where the comparator is inconsistent. From 32 elements on Java merges runs instead; here the
//! binary insertion sort continues, which gives the same order whenever the comparator is a
//! consistent total preorder (the only case the data has reached).

use std::cmp::Ordering;

/// Stable sort of `a` by `c`, in the order `java.util.Arrays.sort` produces for fewer than 32
/// elements. Never panics on an inconsistent comparator.
pub fn java_sort<T, F: FnMut(&T, &T) -> Ordering>(a: &mut [T], mut c: F) {
    let n = a.len();
    if n < 2 {
        return;
    }
    let run = count_run_and_make_ascending(a, &mut c);
    binary_sort(a, run, &mut c);
}

/// `TimSort.countRunAndMakeAscending`: the length of the leading run; a strictly descending run
/// is reversed in place (strict, so stability holds).
fn count_run_and_make_ascending<T, F: FnMut(&T, &T) -> Ordering>(a: &mut [T], c: &mut F) -> usize {
    let hi = a.len();
    let mut run_hi = 1;
    if run_hi == hi {
        return 1;
    }
    if c(&a[run_hi], &a[0]) == Ordering::Less {
        run_hi += 1;
        while run_hi < hi && c(&a[run_hi], &a[run_hi - 1]) == Ordering::Less {
            run_hi += 1;
        }
        a[..run_hi].reverse();
    } else {
        run_hi += 1;
        while run_hi < hi && c(&a[run_hi], &a[run_hi - 1]) != Ordering::Less {
            run_hi += 1;
        }
    }
    run_hi
}

/// `TimSort.binarySort`: inserts each element from `start` on into the sorted prefix, after any
/// equal elements (stable).
fn binary_sort<T, F: FnMut(&T, &T) -> Ordering>(a: &mut [T], mut start: usize, c: &mut F) {
    if start == 0 {
        start = 1;
    }
    while start < a.len() {
        let mut left = 0;
        let mut right = start;
        while left < right {
            let mid = (left + right) >> 1;
            if c(&a[start], &a[mid]) == Ordering::Less {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        a[left..=start].rotate_right(1);
        start += 1;
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sorts_and_keeps_equal_elements_in_input_order() {
        let mut v = vec![(3, 'a'), (1, 'b'), (2, 'c'), (1, 'd'), (3, 'e'), (2, 'f')];
        java_sort(&mut v, |x, y| x.0.cmp(&y.0));
        assert_eq!(
            v,
            vec![(1, 'b'), (1, 'd'), (2, 'c'), (2, 'f'), (3, 'a'), (3, 'e')]
        );
    }

    #[test]
    fn descending_run_is_reversed_without_breaking_stability() {
        let mut v = vec![5, 4, 3, 2, 1, 0];
        java_sort(&mut v, |x, y| x.cmp(y));
        assert_eq!(v, vec![0, 1, 2, 3, 4, 5]);
        let mut v: Vec<i32> = (0..40).rev().collect();
        java_sort(&mut v, |x, y| x.cmp(y));
        assert_eq!(v, (0..40).collect::<Vec<_>>());
    }

    #[test]
    fn inconsistent_comparator_does_not_panic() {
        // a < b, b < c, c < a
        let mut v = vec!['a', 'b', 'c'];
        java_sort(&mut v, |x, y| match (x, y) {
            ('a', 'b') | ('b', 'c') | ('c', 'a') => Ordering::Less,
            ('b', 'a') | ('c', 'b') | ('a', 'c') => Ordering::Greater,
            _ => Ordering::Equal,
        });
        assert_eq!(v.len(), 3);
    }
}
