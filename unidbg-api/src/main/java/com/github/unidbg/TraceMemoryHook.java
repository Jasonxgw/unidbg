package com.github.unidbg;

import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.listener.TraceReadListener;
import com.github.unidbg.listener.TraceWriteListener;
import com.github.unidbg.pointer.UnicornPointer;
import org.apache.commons.codec.binary.Hex;
import unicorn.MemHook;
import unicorn.Unicorn;
import unicorn.UnicornException;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * trace memory read
 * Created by zhkl0228 on 2017/5/2.
 */

class TraceMemoryHook implements MemHook {

    private final boolean read;

    TraceMemoryHook(boolean read) {
        this.read = read;
    }

    PrintStream redirect;
    TraceReadListener traceReadListener;
    TraceWriteListener traceWriteListener;

    @Override
    public void hook(Unicorn u, long address, int size, Object user) {
        if (!read) {
            return;
        }

        try {
            byte[] data = u.mem_read(address, size);
            String value;
            if (data.length == 4) {
                value = "0x" + Long.toHexString(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffffL);
            } else {
                value = Hex.encodeHexString(data);
            }
            Emulator<?> emulator = (Emulator<?>) user;
            printMsg("### Memory READ at 0x", emulator, address, size, value);
            if (traceReadListener != null) {
                traceReadListener.onRead(emulator, address, data, value);
            }
        } catch (UnicornException e) {
            throw new IllegalStateException(e);
        }
    }

    private void printMsg(String type, Emulator<?> emulator, long address, int size, String value) {
        RegisterContext context = emulator.getContext();
        UnicornPointer pc = context.getPCPointer();
        UnicornPointer lr = context.getLRPointer();
        PrintStream out = System.out;
        if (redirect != null) {
            out = redirect;
        }
        String sb = type + Long.toHexString(address) + ", data size = " + size + ", data value = " + value +
                " pc=" + pc +
                " lr=" + lr;
        out.println(sb);
    }

    @Override
    public void hook(Unicorn u, long address, int size, long value, Object user) {
        if (read) {
            return;
        }

        try {
            Emulator<?> emulator = (Emulator<?>) user;
            printMsg("### Memory WRITE at 0x", emulator, address, size, "0x" + Long.toHexString(value));
            if (traceWriteListener != null) {
                traceWriteListener.onWrite(emulator, address, size, value);
            }
        } catch (UnicornException e) {
            throw new IllegalStateException(e);
        }
    }

}
