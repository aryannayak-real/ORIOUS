package app.orious.authenticator.ui.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import app.orious.authenticator.R;
import app.orious.authenticator.passwords.PasswordGenerator;

public final class PasswordGeneratorDialog {
    public interface Listener {
        void onPasswordGenerated(String password);
    }

    private PasswordGeneratorDialog() {}

    public static void show(Context context, Listener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_password_generator, null);
        dialog.setContentView(view);

        Slider lengthSlider = view.findViewById(R.id.generator_length);
        TextView lengthValue = view.findViewById(R.id.generator_length_value);
        TextView preview = view.findViewById(R.id.generator_preview);

        MaterialSwitch uppercase = view.findViewById(R.id.generator_uppercase);
        MaterialSwitch lowercase = view.findViewById(R.id.generator_lowercase);
        MaterialSwitch numbers = view.findViewById(R.id.generator_numbers);
        MaterialSwitch symbols = view.findViewById(R.id.generator_symbols);
        MaterialSwitch excludeAmbiguous =
                view.findViewById(R.id.generator_exclude_ambiguous);

        MaterialButton regenerate = view.findViewById(R.id.generator_regenerate);
        MaterialButton usePassword = view.findViewById(R.id.generator_use);

        // Fresh defaults every time this dialog is opened.
        lengthSlider.setValue(16);
        uppercase.setChecked(true);
        lowercase.setChecked(true);
        numbers.setChecked(true);
        symbols.setChecked(true);
        excludeAmbiguous.setChecked(false);

        final String[] generated = {null};

        Runnable updateLength = () -> {
            lengthValue.setText(context.getString(
                    R.string.password_generator_length_value,
                    (int) lengthSlider.getValue()
            ));
        };

        Runnable updateAvailability = () -> {
            int selected = (uppercase.isChecked() ? 1 : 0)
                    + (lowercase.isChecked() ? 1 : 0)
                    + (numbers.isChecked() ? 1 : 0)
                    + (symbols.isChecked() ? 1 : 0);

            boolean canGenerate = selected > 0
                    && selected <= (int) lengthSlider.getValue();

            regenerate.setEnabled(canGenerate);
            usePassword.setEnabled(canGenerate && generated[0] != null);
        };

        Runnable generate = () -> {
            try {
                generated[0] = PasswordGenerator.generate(
                        (int) lengthSlider.getValue(),
                        uppercase.isChecked(),
                        lowercase.isChecked(),
                        numbers.isChecked(),
                        symbols.isChecked(),
                        excludeAmbiguous.isChecked()
                );
                preview.setText(generated[0]);
                usePassword.setEnabled(true);
            } catch (IllegalArgumentException e) {
                generated[0] = null;
                preview.setText(R.string.password_generator_invalid_options);
                usePassword.setEnabled(false);
            }
            updateAvailability.run();
        };

        lengthSlider.addOnChangeListener((slider, value, fromUser) -> {
            updateLength.run();
            generate.run();
        });

        MaterialSwitch[] switches = {
                uppercase, lowercase, numbers, symbols, excludeAmbiguous
        };
        for (MaterialSwitch item : switches) {
            item.setOnCheckedChangeListener((button, checked) -> generate.run());
        }

        regenerate.setOnClickListener(v -> generate.run());

        usePassword.setOnClickListener(v -> {
            if (generated[0] != null && listener != null) {
                listener.onPasswordGenerated(generated[0]);
                dialog.dismiss();
            }
        });

        updateLength.run();
        updateAvailability.run();
        generate.run();

        dialog.show();
    }
}
