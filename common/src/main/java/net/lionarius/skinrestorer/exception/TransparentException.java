package net.lionarius.skinrestorer.exception;

import org.jetbrains.annotations.NotNull;

public class TransparentException extends RuntimeException {
    public TransparentException(@NotNull Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return this.getCause().getMessage();
    }

    @Override
    public String toString() {
        return this.getLocalizedMessage();
    }
}
