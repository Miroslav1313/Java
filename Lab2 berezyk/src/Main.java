import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringJoiner;

// Інтерфейс для функцій
interface Function {
    // Метод для обчислення значення функції для заданого значення x
    double calculate(double x);
    // Метод для обчислення похідної функції
    Function derivative();
    // Метод для отримання рядкового представлення функції з форматуванням чисел
    String toPrettyString(NumberFormat nf);
}

// Клас функцій виду f(x) = const
class Const implements Function {
    public static final Const ZERO = new Const(0);
    public static final Const ONE = new Const(1);
    public static final Const NEGATIVE_ONE = new Const(-1);
    private final double value;

    public Const(double value) {
        this.value = value;
    }

    @Override
    public double calculate(double x) {
        return value;
    }

    @Override
    public Function derivative() {
        // Похідна від константи завжди дорівнює нулю
        return ZERO;
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        return nf.format(value);
    }

    public static Const of(double value) {
        return new Const(value);
    }
}

// Клас функцій виду f(x) = kx
class Linear implements Function {
    public static final Linear X = new Linear(1.0) {
        @Override
        public String toPrettyString(NumberFormat nf) {
            return "x";
        }
    };
    private final double coefficient;

    public Linear(double coefficient) {
        this.coefficient = coefficient;
    }

    @Override
    public double calculate(double x) {
        return x * coefficient;
    }

    @Override
    public Function derivative() {
        // Похідна від лінійної функції - це константа, що дорівнює коефіцієнту
        return new Const(coefficient);
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        return String.format("%s*x", nf.format(coefficient));
    }

    public static Linear of(double coefficient) {
        return new Linear(coefficient);
    }
}

// Абстрактний клас для композитних функцій
abstract class Composite implements Function {
    private final ArrayList<Function> terms;

    public ArrayList<Function> terms() {
        return terms;
    }

    public Composite() {
        terms = new ArrayList<>();
    }

    public Composite(Function... terms) {
        this.terms = new ArrayList<>(Arrays.asList(terms));
    }

    public Composite(ArrayList<Function> terms) {
        this.terms = terms;
    }
}

// Клас для функцій суми
class Sum extends Composite {
    public Sum() {
        super();
    }

    public Sum(Function... terms) {
        super(terms);
    }

    public Sum(ArrayList<Function> terms) {
        super(terms);
    }

    @Override
    public double calculate(double x) {
        double result = 0.0;
        for (Function function : terms()) {
            result += function.calculate(x);
        }
        return result;
    }

    @Override
    public Function derivative() {
        ArrayList<Function> derivativeTerms = new ArrayList<>();
        for (Function function : terms()) {
            // Обчислюємо похідну кожного доданку суми
            derivativeTerms.add(function.derivative());
        }
        // Повертаємо суму похідних доданків
        return new Sum(derivativeTerms);
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        final StringJoiner joiner = new StringJoiner("+");
        for (Function function : terms()) {
            joiner.add(function.toPrettyString(nf));
        }
        return String.format("(%s)", joiner.toString()).replace("+-", "-");
    }

    public static Sum of(Function... terms) {
        return new Sum(terms);
    }
}

// Клас для функцій множення
class Multiplication extends Composite {
    public Multiplication() {
        super();
    }

    public Multiplication(Function... terms) {
        super(terms);
    }

    public Multiplication(ArrayList<Function> terms) {
        super(terms);
    }

    @Override
    public double calculate(double x) {
        double result = 1.0;
        for (Function function : terms()) {
            result *= function.calculate(x);
        }
        return result;
    }

    @Override
    public Function derivative() {
        ArrayList<Function> derivativeTerms = new ArrayList<>();
        for (int i = 0; i < terms().size(); i++) {
            ArrayList<Function> multipliedTerms = new ArrayList<>(terms());
            Function currentTerm = multipliedTerms.remove(i);
            Function currentTermDerivative = currentTerm.derivative();
            // Перевірка, чи додаємо похідну до множення
            if (!(currentTermDerivative instanceof Const) || ((Const) currentTermDerivative).calculate(0) != 0) {
                multipliedTerms.add(i, currentTermDerivative);
                derivativeTerms.add(new Multiplication(multipliedTerms));
            }
        }
        // Повертаємо суму доданків з похідними
        return new Sum(derivativeTerms);
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        final StringJoiner joiner = new StringJoiner("*");
        for (Function function : terms()) {
            joiner.add(function.toPrettyString(nf));
        }
        return String.format("(%s)", joiner.toString());
    }

    public static Multiplication of(Function... terms) {
        return new Multiplication(terms);
    }
}

// Клас для функцій степені
class Power extends Composite {
    private final Function base;
    private final double exponent;

    public Power(Function base, double exponent) {
        super(base);
        this.base = base;
        this.exponent = exponent;
    }

    @Override
    public double calculate(double x) {
        double baseValue = base.calculate(x);
        return Math.pow(baseValue, exponent);
    }

    @Override
    public Function derivative() {
        return Multiplication.of(
                Const.of(exponent),
                Power.of(base, exponent - 1),
                base.derivative()
        );
    }



    @Override
    public String toPrettyString(NumberFormat nf) {
        String baseString = base.toPrettyString(nf);
        String exponentString = nf.format(exponent);
        return String.format("(%s^%s)", baseString, exponentString);
    }


    public static Power of(Function base, double exponent) {
        return new Power(base, exponent);
    }
}




// Класс для функций корня
class Sqrt extends Composite {
    private final Function base;

    public Sqrt(Function base) {
        super(base);
        this.base = base;
    }

    @Override
    public double calculate(double x) {
        double baseValue = base.calculate(x);
        return Math.sqrt(baseValue);
    }

    @Override
    public Function derivative() {
        Function base = terms().get(0);
        Function baseDerivative = base.derivative();
        // Определение производной для функции корня
        return Multiplication.of(
                Const.of(0.5),
                Power.of(base, -0.5),
                baseDerivative
        );
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        return String.format("sqrt(%s)", base.toPrettyString(nf));
    }

    public static Sqrt of(Function base) {
        return new Sqrt(base);
    }
}



// Класс для функций модуля
class Abs extends Composite {
    private final Function base;

    public Abs(Function base) {
        super(base);
        this.base = base;
    }

    @Override
    public double calculate(double x) {
        double baseValue = base.calculate(x);
        return Math.abs(baseValue);
    }

    @Override
    public Function derivative() {
        Function baseDerivative = base.derivative();

        // Определение производной для функции модуля
        return Multiplication.of(
                Division.of(
                        base,
                        Abs.of(base)
                ),
                baseDerivative
        );
    }


    @Override
    public String toPrettyString(NumberFormat nf) {
        return String.format("|%s|", base.toPrettyString(nf));
    }

    public static Abs of(Function base) {
        return new Abs(base);
    }
}

// Класс для функций синуса
class Sin extends Composite {
    private final Function base;

    public Sin(Function base) {
        super(base);
        this.base = base;
    }

    @Override
    public double calculate(double x) {
        double baseValue = base.calculate(x);
        return Math.sin(baseValue);
    }

    @Override
    public Function derivative() {
        Function baseDerivative = base.derivative();
        // Определение производной для функции синуса
        return Multiplication.of(
                Cos.of(base),
                baseDerivative
        );
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        return String.format("sin(%s)", base.toPrettyString(nf));
    }

    public static Sin of(Function base) {
        return new Sin(base);
    }
}



// Класс для функций косинуса
class Cos extends Composite {
    public Cos(Function base) {
        super(base);
    }

    @Override
    public double calculate(double x) {
        if (!terms().isEmpty()) {
            double baseValue = terms().get(0).calculate(x);
            return Math.cos(baseValue);
        }
        return 0;
    }

    @Override
    public Function derivative() {
        if (!terms().isEmpty()) {
            Function base = terms().get(0);
            Function baseDerivative = base.derivative();
            // Определение производной для функции косинуса
            return Multiplication.of(
                    Const.of(-1),
                    Sin.of(base),
                    baseDerivative
            );
        }
        return Const.ZERO;
    }


    @Override
    public String toPrettyString(NumberFormat nf) {
        return String.format("cos(%s)", terms().isEmpty() ? "" : terms().get(0).toPrettyString(nf));
    }

    public static Cos of(Function base) {
        return new Cos(base);
    }
}

// Класс для функций тангенса
class Tan extends Composite {
    private final Function base;

    public Tan(Function base) {
        super(base);
        this.base = base;
    }

    @Override
    public double calculate(double x) {
        double baseValue = base.calculate(x);
        return Math.tan(baseValue);
    }

    @Override
    public Function derivative() {
        Function base = terms().get(0);
        Function baseDerivative = base.derivative();
        // Определение производной для функции тангенса
        return Multiplication.of(
                Power.of(Cos.of(base), -2),
                baseDerivative
        );
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        return String.format("tan(%s)", base.toPrettyString(nf));
    }

    public static Tan of(Function base) {
        return new Tan(base);
    }
}

class Division extends Composite {
    private final Function numerator;
    private final Function denominator;

    public Division(Function numerator, Function denominator) {
        super(numerator, denominator);
        this.numerator = numerator;
        this.denominator = denominator;
    }

    @Override
    public double calculate(double x) {
        double numeratorValue = numerator.calculate(x);
        double denominatorValue = denominator.calculate(x);
        if (denominatorValue == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return numeratorValue / denominatorValue;
    }

    @Override
    public Function derivative() {
        Function numeratorDerivative = numerator.derivative();
        Function denominatorDerivative = denominator.derivative();
        // Визначення похідної для ділення функцій
        return Division.of(
                Difference.of(
                        Multiplication.of(numeratorDerivative, denominator),
                        Multiplication.of(numerator, denominatorDerivative)
                ),
                Power.of(denominator, 2)
        );
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        return String.format("(%s / %s)", numerator.toPrettyString(nf), denominator.toPrettyString(nf));
    }

    public static Division of(Function numerator, Function denominator) {
        return new Division(numerator, denominator);
    }
}

// Клас для функцій логарифму
class Ln extends Composite {
    private final Function base;

    public Ln(Function base) {
        super(base);
        this.base = base;
    }

    @Override
    public double calculate(double x) {
        double baseValue = base.calculate(x);
        if (baseValue <= 0) {
            throw new ArithmeticException("Logarithm of non-positive number");
        }
        return Math.log(baseValue);
    }

    @Override
    public Function derivative() {
        Function baseDerivative = base.derivative();
        // Визначення похідної для функції логарифму
        return Division.of(
                baseDerivative,
                base
        );
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        return String.format("ln(%s)", base.toPrettyString(nf));
    }

    public static Ln of(Function base) {
        return new Ln(base);
    }
}

// Клас для функцій різниці
class Difference extends Composite {
    public Difference() {
        super();
    }

    public Difference(Function... terms) {
        super(terms);
    }

    public Difference(ArrayList<Function> terms) {
        super(terms);
    }

    @Override
    public double calculate(double x) {
        double result = terms().get(0).calculate(x);
        for (int i = 1; i < terms().size(); i++) {
            result -= terms().get(i).calculate(x);
        }
        return result;
    }

    @Override
    public Function derivative() {
        ArrayList<Function> derivativeTerms = new ArrayList<>();
        for (Function function : terms()) {
            // Обчислюємо похідну кожного доданку різниці
            derivativeTerms.add(function.derivative());
        }
        // Повертаємо різницю похідних доданків
        return new Difference(derivativeTerms);
    }

    @Override
    public String toPrettyString(NumberFormat nf) {
        final StringJoiner joiner = new StringJoiner("-");
        for (Function function : terms()) {
            joiner.add(function.toPrettyString(nf));
        }
        return String.format("(%s)", joiner.toString());
    }

    public static Difference of(Function... terms) {
        return new Difference(terms);
    }
}

public class Main {
    public static void main(String[] args) {
        double x0 = 0.1;

        //  2*(cos(x))^3 – |(-3)*tg(sqrt(x))|
        final Function expression1 =
                Difference.of(
                        Multiplication.of(
                                Const.of(2),
                                Power.of(Cos.of(Linear.X), 3)
                        ),
                        Abs.of(
                                Multiplication.of(
                                        Const.of(-3),
                                        Tan.of(Sqrt.of(Linear.X))

                                )
                        )
                );
        final NumberFormat nf = NumberFormat.getInstance();
        System.out.format("\nf1(x) = %s", expression1.toPrettyString(nf)).println();
        System.out.format("f1'(x) = %s", expression1.derivative().toPrettyString(nf)).println();
        System.out.format("f1(0.1) = %f", expression1.calculate(x0)).println();
        System.out.format("f1'(0.1) = %f", expression1.derivative().calculate(x0)).println();

        // (2*x)/((ln(x+3)^3)^2)
        final Function expression2 =
                Division.of(
                        Multiplication.of(
                                Const.of(2),
                                Linear.X
                        ),
                        Power.of(
                                Ln.of(
                                        Power.of(
                                                Sum.of(
                                                        Linear.X,
                                                        Const.of(3)
                                                ),
                                                3
                                        )
                                ),
                                2
                        )

                );

        System.out.format("\nf2(x) = %s", expression2.toPrettyString(nf)).println();
        System.out.format("f2'(x) = %s", expression2.derivative().toPrettyString(nf)).println();
        System.out.format("f2(0.1) = %f", expression2.calculate(x0)).println();
        System.out.format("f2'(0.1) = %f", expression2.derivative().calculate(x0)).println();
    }
}
