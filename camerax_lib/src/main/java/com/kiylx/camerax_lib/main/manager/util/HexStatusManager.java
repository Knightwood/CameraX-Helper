package com.kiylx.camerax_lib.main.manager.util;

/**
 * 首先定义一个 “状态集” 变量，用于存放 “当前状态集”，例如：
 * private int STATUSES;
 * <p>
 * 然后定义十六进制状态常量，和 “模式状态集”，例如：
 * <p>
 * private final int STATUS_1 = 0x0001; <br/>
 * private final int STATUS_2 = 0x0002; <br/>
 * private final int STATUS_3 = 0x0004; <br/>
 * private final int STATUS_4 = 0x0008; <br/>
 * private final int STATUS_5 = 0x0010; <br/>
 * private final int STATUS_6 = 0x0020; <br/>
 * private final int STATUS_7 = 0x0040; <br/>
 * private final int STATUS_8 = 0x0080; <br/>
 * <p>
 * private final int MODE_A = STATUS_1 | STATUS_2 | STATUS_3; <br/>
 * private final int MODE_B = STATUS_1 | STATUS_4 | STATUS_5 | STATUS_6; <br/>
 * private final int MODE_C = STATUS_1 | STATUS_7 | STATUS_8; <br/>
 * <p>
 * <p>
 * 当需往 “状态集” 添加状态时，就通过 “或” 运算。例如： <br/>
 * STATUSES | STATUS_1 <br/>
 * 当需从 “状态集” 移除状态时，就通过 “取反” 运算。例如： <br/>
 * STATUSES & ~ STATUS_1 <br/>
 * 当需判断 “状态集” 是否包含某状态时，就通过 “与” 运算。结果为 0 即代表无，反之有。 <br/>
 * public static boolean isStatusEnabled(int statuses, int status) { <br/>
 * return (statuses & status) != 0; <br/>
 * } <br/>
 * 当需切换模式时，可直接将预先定义的 “模式状态集” 赋予给 “状态集” 变量。例如： <br/>
 * STATUSES = MODE_A; <br/>
 */
public class HexStatusManager {

    /**
     * 将新状态添加到已有状态中
     *
     * @param status
     * @param value
     * @return 如果要添加的状态已经存在，则返回原状态，否则返回新状态
     */
    public static int add(int status, int value) {
        if (!isContain(status, value)) {
            return status | value;
        }
        return status;
    }

    /**
     * 将状态从已有状态中移除
     *
     * @param status
     * @param value
     * @return 如果要移除的状态不存在，则返回原状态，否则返回新状态
     */
    public static int remove(int status, int value) {
        if (isContain(status, value)) {
            return status & ~value;
        }
        return status;
    }

    /**
     * 判断状态集是否包含某状态
     *
     * @param status
     * @param value
     * @return
     */
    public static boolean isContain(int status, int value) {
        return (status & value) != 0;
    }

    /**
     * 状态中是否包含后面所有状态值，有一个不包含就返回false
     *
     * @param status
     * @param value
     * @return
     */
    public static boolean isAllContain(int status, int... value) {
        boolean allContain = true;
        for (int i : value) {
            if (!isContain(status, i)) {
                allContain = false;
                break;
            }
        }
        return allContain;
    }
}
