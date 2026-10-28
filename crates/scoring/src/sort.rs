//! A stable sort that never panics on a comparator that is not a total order.
//!
//! [`crate::assign_places`] sorts the series rows with a comparator whose tiebreak (GSI 14.5, D5 in
//! `test_audit_and_plan.md`) skips the races where *either* boat was the RC volunteer, so it is not transitive in
//! general: the race it skips depends on the pair. `slice::sort_by` may panic on such a comparator. [`java_sort`]
//! is TimSort's short-array path, a leading run (`countRunAndMakeAscending`) followed by a binary insertion sort
//! (`binarySort`), which is the order the approved golden standings were recorded in, even where the comparator
//! is inconsistent. TimSort merges runs from 32 elements on; here the binary insertion sort continues, which gives
//! the same order whenever the comparator is a consistent total preorder (the only case the data has reached).

use std::cmp::Ordering;

/// Sorts `a` by `c`, stably, in the order TimSort's binary insertion sort produces.
///
/// Never panics on an inconsistent comparator. For fewer than 32 elements the order equals
/// `java.util.Arrays.sort`'s, the order the golden standings were recorded in.
pub fn java_sort<T, F: FnMut(&T, &T) -> Ordering>(a: &mut [T], mut c: F) {
    let n = a.len();
    if n < 2 {
        return;
    }
    let run = count_run_and_make_ascending(a, &mut c);
    binary_sort(a, run, &mut c);
}

/// Returns the length of the leading run of `a`, reversing it in place if it is strictly descending.
///
/// Only a strictly descending run is reversed, so equal elements keep their order. TimSort's
/// `countRunAndMakeAscending`.
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

/// Inserts each element from `start` on into the sorted prefix, after any equal elements (stable).
///
/// TimSort's `binarySort`.
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
