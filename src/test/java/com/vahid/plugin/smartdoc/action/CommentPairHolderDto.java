package com.vahid.plugin.smartdoc.action;


public class CommentPairHolderDto {
    private String actual;
    private String expected;

    private CommentPairHolderDto(Builder builder) {
        this.actual = builder.actual;
        this.expected = builder.expected;
    }

    public static Builder Builder() {
        return new Builder();
    }

    public static class Builder {
        private String actual;
        private String expected;


        public Builder actual(String actual) {
            this.actual = actual;
            return this;
        }

        public Builder expected(String expected) {
            this.expected = expected;
            return this;
        }

        public CommentPairHolderDto build() {
            return new CommentPairHolderDto(this);
        }

    }

    public CommentPairHolderDto setActual(String actual) {
        this.actual = actual;
        return this;
    }

    public String getActual() {
        return actual;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    public String getExpected() {
        return expected;
    }
}
