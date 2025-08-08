package org.schimodie.common.data;

public record Tuple3<T1, T2, T3>(T1 t1, T2 t2, T3 t3) {
    public static <T1, T2, T3> Tuple3<T1, T2, T3> of(T1 t1, T2 t2, T3 t3) {
        return new Tuple3<>(t1, t2, t3);
    }
}
