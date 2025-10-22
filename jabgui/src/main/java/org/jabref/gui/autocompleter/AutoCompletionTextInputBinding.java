/**
 * Copyright (c) 2014, 2015, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jabref.gui.autocompleter;

import java.util.Collection;
import java.util.Objects;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.TextInputControl;
import javafx.util.Callback;
import javafx.util.StringConverter;

import org.jabref.gui.util.UiTaskExecutor;

import org.controlsfx.control.textfield.AutoCompletionBinding;

/**
 * Represents a binding between a text input control and an auto-completion popup
 * This class is a slightly modified version of {@link org.controlsfx.control.textfield.AutoCompletionBinding}
 * that works with general text input controls instead of just text fields.
 */
public class AutoCompletionTextInputBinding<T> extends AutoCompletionBinding<T> {

    /**
     * String converter to be used to convert suggestions to strings.
     */
    private final StringConverter<T> converter;
    private final AutoCompletionStrategy inputAnalyzer;
    private final ChangeListener<String> textChangeListener = (_, _, newText) -> {
        if (getCompletionTarget().isFocused()) {
            setUserInputText(newText);
        }
    };
    private boolean showOnFocus;
    private final ChangeListener<Boolean> focusChangedListener = (_, _, newFocused) -> {
        if (newFocused) {
            if (showOnFocus) {
                setUserInputText(getCompletionTarget().getText());
            }
        } else {
            hidePopup();
        }
    };

    private AutoCompletionTextInputBinding(final Builder<T> builder) {
        super(builder.textInputControl, builder.suggestionProvider, builder.converter);
        this.converter = builder.converter;
        this.inputAnalyzer = builder.inputAnalyzer;
        this.showOnFocus = builder.showOnFocus;
        getCompletionTarget().textProperty().addListener(textChangeListener);
        getCompletionTarget().focusedProperty().addListener(focusChangedListener);
    }

    private static <T> StringConverter<T> defaultStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(T t) {
                return t == null ? null : t.toString();
            }

            @SuppressWarnings("unchecked")
            @Override
            public T fromString(String string) {
                return (T) string;
            }
        };
    }

    /**
     * Returns a new {@link AutoCompletionTextInputBinding.Builder} instance for constructing a {@code AutoCompletionTextInputBinding}.
     *
     * @return A new Builder instance initialized with default components (empty database and metadata).
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    private void setUserInputText(String newText) {
        if (newText == null) {
            newText = "";
        }
        AutoCompletionInput input = inputAnalyzer.analyze(newText);
        UiTaskExecutor.runInJavaFXThread(() -> setUserInput(input.getUnfinishedPart()));
    }

    @Override
    public TextInputControl getCompletionTarget() {
        return (TextInputControl) super.getCompletionTarget();
    }

    @Override
    public void dispose() {
        getCompletionTarget().textProperty().removeListener(textChangeListener);
        getCompletionTarget().focusedProperty().removeListener(focusChangedListener);
    }

    @Override
    protected void completeUserInput(T completion) {
        String completionText = converter.toString(completion);
        String inputText = getCompletionTarget().getText();
        if (inputText == null) {
            inputText = "";
        }
        AutoCompletionInput input = inputAnalyzer.analyze(inputText);
        String newText = input.getPrefix() + completionText;
        getCompletionTarget().setText(newText);
        getCompletionTarget().positionCaret(newText.length());
    }

    public void setShowOnFocus(boolean showOnFocus) {
        this.showOnFocus = showOnFocus;
    }

    /**
     * A Builder class used to construct and configure an {@code AutoCompletionTextInputBinding}.
     *
     * @param <T> The type of the suggestion objects provided by the suggestion provider.
     */
    public static class Builder<T> {
        private TextInputControl textInputControl;
        private Callback<ISuggestionRequest, Collection<T>> suggestionProvider;
        private StringConverter<T> converter = AutoCompletionTextInputBinding.defaultStringConverter();
        private AutoCompletionStrategy inputAnalyzer = new ReplaceStrategy();
        private boolean showOnFocus = false; // Assuming default is false

        /**
         * Sets the required TextInputControl.
         *
         * @param textInputControl The TextInputControl to bind to.
         * @return The builder instance.
         */
        public Builder<T> forTextInputControl(TextInputControl textInputControl) {
            this.textInputControl = textInputControl;
            return this;
        }

        /**
         * Sets the required suggestion provider.
         *
         * @param suggestionProvider The callback to retrieve suggestions.
         * @return The builder instance.
         */
        @SuppressWarnings("unchecked")
        public Builder<T> usingSuggestionProvider(Callback<ISuggestionRequest, Collection<?>> suggestionProvider) {
            this.suggestionProvider = (Callback<ISuggestionRequest, Collection<T>>) (Callback<?, ?>) suggestionProvider;
            return this;
        }

        /**
         * Sets the optional StringConverter.
         *
         * @param converter The converter for suggestions to strings.
         * @return The builder instance.
         */
        public Builder<T> withStringConverter(StringConverter<T> converter) {
            this.converter = converter;
            return this;
        }

        /**
         * Sets the optional AutoCompletionStrategy.
         *
         * @param inputAnalyzer The strategy to analyze the input text.
         * @return The builder instance.
         */
        public Builder<T> withInputAnalyzer(AutoCompletionStrategy inputAnalyzer) {
            this.inputAnalyzer = inputAnalyzer;
            return this;
        }

        /**
         * Builds the AutoCompletionTextInputBinding object.
         *
         * @return A new instance of AutoCompletionTextInputBinding.
         */
        public AutoCompletionTextInputBinding<T> build() {
            Objects.requireNonNull(textInputControl, "TextInputControl is required.");
            Objects.requireNonNull(suggestionProvider, "SuggestionProvider is required.");

            if (converter == null) {
                converter = AutoCompletionTextInputBinding.defaultStringConverter();
            }
            if (inputAnalyzer == null) {
                inputAnalyzer = new ReplaceStrategy(); // Assuming ReplaceStrategy is the default
            }

            return new AutoCompletionTextInputBinding<>(this);
        }
    }
}
