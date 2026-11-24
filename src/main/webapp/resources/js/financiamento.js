(() => {
    'use strict';

    const storageKey = 'financiamento-imobiliario-v1';
    const defaults = {
        amount: 350000,
        annualRate: 10.5,
        selicRate: 0,
        ipcaRate: 0,
        propertyAppreciationRate: 0,
        trRate: 0,
        months: 360,
        birthDate: '',
        propertyValue: 450000,
        dfiRate: 0.01,
        monthlyExtra: 0,
        fgtsBalance: 0,
        grossSalary: 0,
        strategy: 'term',
        fgtsMode: 'none',
        fgtsStrategy: 'term',
        mipRates: [0.01, 0.012, 0.015, 0.02, 0.03, 0.045, 0.065, 0.09, 0.13, 0.19, 0.27, 0.35],
        extraByMonth: {}
    };
    const state = {...defaults};
    function control(id) {
        return document.getElementById(`${id}_input`) || document.getElementById(id);
    }

    const fields = {
        amount: control('loanAmount'),
        annualRate: control('annualRate'),
        selicRate: control('selicRate'),
        ipcaRate: control('ipcaRate'),
        propertyAppreciationRate: control('propertyAppreciationRate'),
        trRate: control('trRate'),
        months: control('termMonths'),
        birthDate: control('birthDate'),
        propertyValue: control('propertyValue'),
        dfiRate: control('dfiRate'),
        monthlyExtra: control('monthlyExtra'),
        strategy: control('extraStrategy'),
        fgtsBalance: control('fgtsBalance'),
        grossSalary: control('grossSalary'),
        fgtsMode: control('fgtsMode'),
        fgtsStrategy: control('fgtsStrategy')
    };
    fields.strategy.dataset.widgetVar = 'financingExtraStrategy';
    fields.fgtsMode.dataset.widgetVar = 'financingFgtsMode';
    fields.fgtsStrategy.dataset.widgetVar = 'financingFgtsStrategy';
    const mipAgeLimits = [25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 120];
    const message = document.getElementById('financingMessage');
    const money = new Intl.NumberFormat('pt-BR', {style: 'currency', currency: 'BRL'});
    const number = new Intl.NumberFormat('pt-BR', {maximumFractionDigits: 3});
    const percentage = new Intl.NumberFormat('pt-BR', {maximumFractionDigits: 2});
    const svgNamespace = 'http://www.w3.org/2000/svg';
    let schedules = {};
    let activeSystem = 'SAC';
    let storageWarning = false;
    let calculationDirty = false;

    function showMessage(text, isError = false) {
        message.textContent = text;
        message.classList.toggle('financing-message-error', isError);
    }

    function markCalculationDirty() {
        calculationDirty = true;
        document.getElementById('calculationStatus').textContent =
            'Há alterações pendentes. Clique em “Calcular simulação e valorização do imóvel” para atualizar os resultados.';
    }

    function roundMoney(value) {
        return Math.round((value + Number.EPSILON) * 100) / 100;
    }

    function loadState() {
        try {
            const stored = localStorage.getItem(storageKey);
            if (!stored) {
                renderMipRates();
                return;
            }
            const parsed = JSON.parse(stored);
            for (const key of [
                'amount', 'annualRate', 'selicRate', 'ipcaRate', 'propertyAppreciationRate',
                'trRate', 'months', 'propertyValue',
                'dfiRate', 'monthlyExtra', 'fgtsBalance', 'grossSalary'
            ]) {
                if (typeof parsed[key] === 'number' && Number.isFinite(parsed[key])) state[key] = parsed[key];
            }
            if (typeof parsed.birthDate === 'string') state.birthDate = parsed.birthDate;
            if (parsed.strategy === 'term' || parsed.strategy === 'payment') state.strategy = parsed.strategy;
            if (parsed.fgtsMode === 'none' || parsed.fgtsMode === 'amortize' || parsed.fgtsMode === 'installments') {
                state.fgtsMode = parsed.fgtsMode;
            }
            if (parsed.fgtsStrategy === 'term' || parsed.fgtsStrategy === 'payment') {
                state.fgtsStrategy = parsed.fgtsStrategy;
            }
            if (Array.isArray(parsed.mipRates) && parsed.mipRates.length === mipAgeLimits.length
                && parsed.mipRates.every(rate => Number.isFinite(rate) && rate >= 0 && rate <= 1)) {
                state.mipRates = parsed.mipRates;
            }
            if (parsed.extraByMonth && typeof parsed.extraByMonth === 'object' && !Array.isArray(parsed.extraByMonth)) {
                state.extraByMonth = Object.fromEntries(
                    Object.entries(parsed.extraByMonth).filter(([, value]) => Number.isFinite(value) && value >= 0)
                );
            }
        } catch (error) {
            storageWarning = true;
        }
        setFieldValue(fields.amount, state.amount);
        setFieldValue(fields.annualRate, state.annualRate);
        setFieldValue(fields.selicRate, state.selicRate);
        setFieldValue(fields.ipcaRate, state.ipcaRate);
        setFieldValue(fields.propertyAppreciationRate, state.propertyAppreciationRate);
        setFieldValue(fields.trRate, state.trRate);
        setFieldValue(fields.months, state.months);
        setFieldValue(fields.birthDate, state.birthDate);
        setFieldValue(fields.propertyValue, state.propertyValue);
        setFieldValue(fields.dfiRate, state.dfiRate);
        setFieldValue(fields.monthlyExtra, state.monthlyExtra);
        setFieldValue(fields.strategy, state.strategy);
        setFieldValue(fields.fgtsBalance, state.fgtsBalance);
        setFieldValue(fields.grossSalary, state.grossSalary);
        setFieldValue(fields.fgtsMode, state.fgtsMode);
        setFieldValue(fields.fgtsStrategy, state.fgtsStrategy);
        renderMipRates();
    }

    function setFieldValue(field, value) {
        const widgetVar = field.dataset.widgetVar;
        if (widgetVar && typeof window.PF === 'function') {
            const widget = window.PF(widgetVar);
            if (widget && typeof widget.selectValue === 'function') {
                widget.selectValue(String(value));
                return;
            }
        }
        field.value = value;
    }

    function saveState() {
        try {
            localStorage.setItem(storageKey, JSON.stringify(state));
            return true;
        } catch (error) {
            showMessage('Não foi possível salvar a simulação neste navegador.', true);
            return false;
        }
    }

    function readInputs() {
        state.amount = Number(fields.amount.value);
        state.annualRate = Number(fields.annualRate.value);
        state.selicRate = Number(fields.selicRate.value);
        state.ipcaRate = Number(fields.ipcaRate.value);
        state.propertyAppreciationRate = Number(fields.propertyAppreciationRate.value);
        state.trRate = Number(fields.trRate.value);
        state.months = Number(fields.months.value);
        state.birthDate = fields.birthDate.value;
        state.propertyValue = Number(fields.propertyValue.value);
        state.dfiRate = Number(fields.dfiRate.value);
        state.monthlyExtra = Number(fields.monthlyExtra.value);
        state.strategy = fields.strategy.value;
        state.fgtsBalance = Number(fields.fgtsBalance.value);
        state.grossSalary = Number(fields.grossSalary.value);
        state.fgtsMode = fields.fgtsMode.value;
        state.fgtsStrategy = fields.fgtsStrategy.value;
        state.mipRates = Array.from(document.querySelectorAll('.financing-mip-rate'), input => Number(input.value));
        if (!Number.isFinite(state.amount) || state.amount <= 0 || state.amount > 1000000000) {
            throw new Error('Informe um valor financiado entre R$ 1,00 e R$ 1 bilhão.');
        }
        if (!Number.isFinite(state.annualRate) || state.annualRate < 0 || state.annualRate > 100) {
            throw new Error('Informe uma taxa anual entre 0% e 100%.');
        }
        if (!Number.isFinite(state.selicRate) || state.selicRate < 0 || state.selicRate > 100) {
            throw new Error('Informe uma taxa média anual da SELIC entre 0% e 100%.');
        }
        if (!Number.isFinite(state.ipcaRate) || state.ipcaRate < 0 || state.ipcaRate > 100) {
            throw new Error('Informe uma taxa média anual do IPCA entre 0% e 100%.');
        }
        if (!Number.isFinite(state.propertyAppreciationRate)
            || state.propertyAppreciationRate < 0 || state.propertyAppreciationRate > 100) {
            throw new Error('Informe uma valorização anual estimada do imóvel entre 0% e 100%.');
        }
        if (!Number.isFinite(state.trRate) || state.trRate < 0 || state.trRate > 30) {
            throw new Error('Informe uma TR anual entre 0% e 30%.');
        }
        if (!Number.isInteger(state.months) || state.months < 1 || state.months > 600) {
            throw new Error('Informe um prazo entre 1 e 600 meses.');
        }
        if (!state.birthDate || Number.isNaN(Date.parse(`${state.birthDate}T00:00:00`))) {
            throw new Error('Informe uma data de nascimento válida para estimar o MIP.');
        }
        const birthDate = new Date(`${state.birthDate}T00:00:00`);
        const today = new Date();
        if (birthDate >= today || ageOnDate(birthDate, today) > 120 || ageOnDate(birthDate, today) < 18) {
            throw new Error('A data de nascimento deve corresponder a um mutuário com idade entre 18 e 120 anos.');
        }
        if (!Number.isFinite(state.propertyValue) || state.propertyValue <= 0 || state.propertyValue > 1000000000) {
            throw new Error('Informe um valor do imóvel entre R$ 1,00 e R$ 1 bilhão.');
        }
        if (!Number.isFinite(state.dfiRate) || state.dfiRate < 0 || state.dfiRate > 1) {
            throw new Error('Informe uma taxa DFI mensal entre 0% e 1%.');
        }
        if (!Number.isFinite(state.monthlyExtra) || state.monthlyExtra < 0 || state.monthlyExtra > 1000000000) {
            throw new Error('O abatimento mensal deve ser um valor positivo ou zero.');
        }
        if (!Number.isFinite(state.fgtsBalance) || state.fgtsBalance < 0 || state.fgtsBalance > 1000000000) {
            throw new Error('Informe um saldo FGTS positivo ou zero.');
        }
        if (!Number.isFinite(state.grossSalary) || state.grossSalary < 0 || state.grossSalary > 1000000000) {
            throw new Error('Informe um salário bruto mensal positivo ou zero.');
        }
        if (state.mipRates.length !== mipAgeLimits.length
            || state.mipRates.some(rate => !Number.isFinite(rate) || rate < 0 || rate > 1)) {
            throw new Error('As taxas MIP devem estar entre 0% e 1% ao mês.');
        }
    }

    function paymentFor(balance, monthlyRate, months) {
        if (monthlyRate === 0) return roundMoney(balance / months);
        const growth = Math.pow(1 + monthlyRate, months);
        return roundMoney(balance * monthlyRate * growth / (growth - 1));
    }

    function renderMipRates() {
        const body = document.getElementById('mipRateRows');
        if (!body) return;
        const fragment = document.createDocumentFragment();
        mipAgeLimits.forEach((maxAge, index) => {
            const row = document.createElement('tr');
            const ageCell = document.createElement('td');
            ageCell.textContent = index === 0 ? 'Até 25 anos' : `${mipAgeLimits[index - 1] + 1}–${maxAge} anos`;
            const rateCell = document.createElement('td');
            const input = document.createElement('input');
            input.type = 'number';
            input.min = '0';
            input.max = '1';
            input.step = '0.001';
            input.value = state.mipRates[index];
            input.className = 'financing-mip-rate';
            input.setAttribute('aria-label', `Taxa MIP mensal até ${maxAge} anos, em percentual`);
            input.addEventListener('change', markCalculationDirty);
            rateCell.append(input);
            row.append(ageCell, rateCell);
            fragment.append(row);
        });
        body.replaceChildren(fragment);
    }

    function ageOnDate(birthDate, date) {
        let age = date.getFullYear() - birthDate.getFullYear();
        const birthdayThisYear = new Date(date.getFullYear(), birthDate.getMonth(), birthDate.getDate());
        if (date < birthdayThisYear) age--;
        return age;
    }

    function mipRateForAge(age) {
        const index = mipAgeLimits.findIndex(maxAge => age <= maxAge);
        return state.mipRates[index === -1 ? state.mipRates.length - 1 : index];
    }

    function calculateSchedule(system) {
        const monthlyRate = Math.pow(1 + state.annualRate / 100, 1 / 12) - 1;
        const monthlyTrRate = Math.pow(1 + state.trRate / 100, 1 / 12) - 1;
        const initialPayment = paymentFor(state.amount, monthlyRate, state.months);
        const initialSacAmortization = roundMoney(state.amount / state.months);
        let balance = roundMoney(state.amount);
        let fgtsBalance = roundMoney(state.fgtsBalance);
        let fgtsInstallmentReserve = 0;
        let fgtsInstallmentShare = 0;
        const monthlyFgtsRate = Math.pow(1.03, 1 / 12) - 1;
        let indexedPropertyValue = roundMoney(state.propertyValue);
        let scheduledPricePayment = initialPayment;
        let scheduledSacAmortization = initialSacAmortization;
        let cumulativeInterest = 0;
        let cumulativeTr = 0;
        let cumulativeInsurance = 0;
        let cumulativeAdmin = 0;
        let cumulativePaid = 0;
        let cumulativeFgtsUsed = 0;
        let totalFgtsUsed = 0;
        let totalFgtsDeposits = 0;
        let totalFgtsYield = 0;
        const contractStart = new Date();
        const contractDay = contractStart.getDate();
        contractStart.setDate(1);
        const rows = [];

        for (let month = 1; month <= state.months && balance > 0; month++) {
            const fgtsYield = roundMoney(fgtsBalance * monthlyFgtsRate);
            fgtsBalance = roundMoney(fgtsBalance + fgtsYield);
            const reservedFgtsYield = roundMoney(fgtsInstallmentReserve * monthlyFgtsRate);
            fgtsInstallmentReserve = roundMoney(fgtsInstallmentReserve + reservedFgtsYield);
            const fgtsDeposit = roundMoney(state.grossSalary * 0.08);
            fgtsBalance = roundMoney(fgtsBalance + fgtsDeposit);
            totalFgtsDeposits = roundMoney(totalFgtsDeposits + fgtsDeposit);
            totalFgtsYield = roundMoney(totalFgtsYield + fgtsYield + reservedFgtsYield);
            const openingBalance = balance;
            const trCorrection = roundMoney(balance * monthlyTrRate);
            balance = roundMoney(balance + trCorrection);
            indexedPropertyValue = roundMoney(indexedPropertyValue * (1 + monthlyTrRate));
            const remainingMonths = state.months - month + 1;
            const interest = roundMoney(balance * monthlyRate);
            const loanMonth = new Date(
                contractStart.getFullYear(),
                contractStart.getMonth() + month - 1,
                1
            );
            const lastDayOfMonth = new Date(
                loanMonth.getFullYear(),
                loanMonth.getMonth() + 1,
                0
            ).getDate();
            loanMonth.setDate(Math.min(contractDay, lastDayOfMonth));
            const age = ageOnDate(new Date(`${state.birthDate}T00:00:00`), loanMonth);
            const mipRate = mipRateForAge(age);
            const mip = roundMoney(balance * (mipRate / 100));
            const dfi = roundMoney(indexedPropertyValue * (state.dfiRate / 100));
            const adminFee = 25;
            const reducingForExtra = state.strategy === 'payment';
            const sacAmortization = reducingForExtra
                ? roundMoney(balance / remainingMonths)
                : scheduledSacAmortization;
            const principalInstallment = system === 'SAC'
                ? sacAmortization + interest
                : reducingForExtra
                    ? paymentFor(balance, monthlyRate, remainingMonths)
                    : roundMoney(scheduledPricePayment * (1 + monthlyTrRate));
            if (system === 'PRICE' && !reducingForExtra) {
                scheduledPricePayment = principalInstallment;
            }
            const finalInstallment = remainingMonths === 1 ? roundMoney(balance + interest) : principalInstallment;
            const amortization = roundMoney(Math.min(balance, Math.max(0, finalInstallment - interest)));
            balance = roundMoney(Math.max(0, balance - amortization));
            const totalInstallment = roundMoney(interest + amortization + mip + dfi + adminFee);
            const fgtsForInstallment = state.fgtsMode === 'installments' && month > 12
                ? roundMoney(Math.min(fgtsInstallmentShare, fgtsInstallmentReserve, interest + amortization))
                : 0;
            fgtsInstallmentReserve = roundMoney(Math.max(0, fgtsInstallmentReserve - fgtsForInstallment));
            const requestedExtra = roundMoney(state.monthlyExtra + (Number(state.extraByMonth[month]) || 0));
            const extra = roundMoney(Math.min(balance, requestedExtra));
            balance = roundMoney(Math.max(0, balance - extra));
            const fgtsForAmortization = state.fgtsMode === 'amortize' && month % 24 === 0
                ? roundMoney(Math.min(balance, fgtsBalance))
                : 0;
            balance = roundMoney(Math.max(0, balance - fgtsForAmortization));
            fgtsBalance = roundMoney(Math.max(0, fgtsBalance - fgtsForAmortization));
            if (state.fgtsMode === 'installments' && month % 12 === 0) {
                fgtsBalance = roundMoney(fgtsBalance + fgtsInstallmentReserve);
                fgtsInstallmentReserve = fgtsBalance;
                fgtsBalance = 0;
                fgtsInstallmentShare = roundMoney(fgtsInstallmentReserve / 12);
            }
            const monthsAfterThisPayment = state.months - month;
            if (fgtsForAmortization > 0 && state.fgtsStrategy === 'payment' && monthsAfterThisPayment > 0) {
                scheduledSacAmortization = roundMoney(balance / monthsAfterThisPayment);
                scheduledPricePayment = paymentFor(balance, monthlyRate, monthsAfterThisPayment);
            }
            cumulativeInterest = roundMoney(cumulativeInterest + interest);
            cumulativeTr = roundMoney(cumulativeTr + trCorrection);
            cumulativeInsurance = roundMoney(cumulativeInsurance + mip + dfi);
            cumulativeAdmin = roundMoney(cumulativeAdmin + adminFee);
            const fgtsUsed = roundMoney(fgtsForInstallment + fgtsForAmortization);
            totalFgtsUsed = roundMoney(totalFgtsUsed + fgtsUsed);
            cumulativeFgtsUsed = roundMoney(cumulativeFgtsUsed + fgtsUsed);
            const cashPayment = roundMoney(totalInstallment - fgtsForInstallment + extra);
            const paid = roundMoney(totalInstallment + extra + fgtsForAmortization);
            cumulativePaid = roundMoney(cumulativePaid + paid);
            rows.push({
                month,
                openingBalance,
                trCorrection,
                installment: totalInstallment,
                principalInstallment: roundMoney(interest + amortization),
                interest,
                amortization,
                age,
                mipRate,
                mip,
                dfi,
                adminFee,
                monthlyExtra: roundMoney(Math.min(extra, state.monthlyExtra)),
                oneTimeExtra: roundMoney(Math.max(0, extra - Math.min(extra, state.monthlyExtra))),
                fgtsDeposit,
                fgtsYield,
                fgtsForInstallment,
                fgtsInstallmentReserve,
                fgtsForAmortization,
                fgtsUsed,
                cumulativeFgtsUsed,
                cashPayment,
                closingBalance: balance,
                fgtsBalance: roundMoney(fgtsBalance + fgtsInstallmentReserve),
                cumulativeInterest,
                cumulativeTr,
                cumulativePaid
            });
        }
        return {
            rows,
            totalInterest: cumulativeInterest,
            totalTr: cumulativeTr,
            totalInsurance: cumulativeInsurance,
            totalAdmin: cumulativeAdmin,
            totalPaid: cumulativePaid,
            totalFgtsUsed,
            totalFgtsDeposits,
            totalFgtsYield,
            remainingFgts: roundMoney(fgtsBalance + fgtsInstallmentReserve),
            duration: rows.length,
            firstInstallment: rows.length ? rows[0].installment : 0,
            lastInstallment: rows.length ? rows[rows.length - 1].installment : 0
        };
    }

    function svgElement(tag, attributes = {}) {
        const element = document.createElementNS(svgNamespace, tag);
        Object.entries(attributes).forEach(([key, value]) => element.setAttribute(key, value));
        return element;
    }

    function drawChart(containerId, datasets, title) {
        const container = document.getElementById(containerId);
        const width = 760;
        const height = 270;
        const padding = {top: 14, right: 18, bottom: 36, left: 86};
        const plotWidth = width - padding.left - padding.right;
        const plotHeight = height - padding.top - padding.bottom;
        const count = Math.max(...datasets.map(dataset => dataset.values.length), 1);
        const values = datasets.flatMap(dataset => dataset.values);
        const minValue = Math.min(0, ...values);
        const maxValue = Math.max(0, ...values);
        const valueRange = maxValue - minValue || 1;
        const svg = svgElement('svg', {
            viewBox: `0 0 ${width} ${height}`,
            role: 'img',
            'aria-label': title,
            focusable: 'false'
        });
        const description = svgElement('desc');
        description.textContent = title;
        svg.append(description);

        for (let tick = 0; tick <= 4; tick++) {
            const y = padding.top + plotHeight * tick / 4;
            const value = maxValue - valueRange * tick / 4;
            const grid = svgElement('line', {
                x1: padding.left, y1: y, x2: width - padding.right, y2: y,
                class: `financing-chart-gridline${value === 0 && minValue < 0 ? ' financing-chart-zero-line' : ''}`
            });
            const label = svgElement('text', {x: padding.left - 10, y: y + 4, 'text-anchor': 'end'});
            label.textContent = money.format(value);
            svg.append(grid, label);
        }

        datasets.forEach(dataset => {
            const points = dataset.values.map((value, index) => {
                const x = padding.left + (count === 1 ? 0 : (index / (count - 1)) * plotWidth);
                const y = padding.top + ((maxValue - value) / valueRange) * plotHeight;
                return `${x},${y}`;
            });
            svg.append(svgElement('polyline', {
                points: points.join(' '),
                class: `financing-chart-line ${dataset.className}`
            }));
        });

        const firstMonth = svgElement('text', {
            x: padding.left, y: height - 8, 'text-anchor': 'middle'
        });
        firstMonth.textContent = '1';
        const lastMonth = svgElement('text', {
            x: width - padding.right, y: height - 8, 'text-anchor': 'middle'
        });
        lastMonth.textContent = String(count);
        const axisLabel = svgElement('text', {
            x: width / 2, y: height - 8, 'text-anchor': 'middle', class: 'financing-chart-axis-label'
        });
        axisLabel.textContent = 'Parcela';
        svg.append(firstMonth, lastMonth, axisLabel);
        container.replaceChildren(svg);
    }

    function updateSummaries() {
        const sac = schedules.SAC;
        const price = schedules.PRICE;
        const projectRate = (annualRate, months, baseAmount = state.amount) => {
            const factor = Math.pow(1 + annualRate / 100, months / 12);
            const projectedAmount = roundMoney(baseAmount * factor);
            return {
                accumulatedRate: (factor - 1) * 100,
                projectedAmount,
                accumulatedAmount: roundMoney(projectedAmount - baseAmount)
            };
        };
        const formatDifference = value => value > 0 ? `+${money.format(value)}` : money.format(value);
        const selicProjection = {
            SAC: projectRate(state.selicRate, sac.duration),
            PRICE: projectRate(state.selicRate, price.duration)
        };
        const ipcaProjection = {
            SAC: projectRate(state.ipcaRate, sac.duration),
            PRICE: projectRate(state.ipcaRate, price.duration)
        };
        const propertyProjection = {
            SAC: projectRate(state.propertyAppreciationRate, sac.duration, state.propertyValue),
            PRICE: projectRate(state.propertyAppreciationRate, price.duration, state.propertyValue)
        };
        document.getElementById('sacInterest').textContent = money.format(sac.totalInterest);
        document.getElementById('priceInterest').textContent = money.format(price.totalInterest);
        document.getElementById('sacSummary').textContent =
            `${sac.duration} parcelas · primeira total ${money.format(sac.rows[0]?.installment || 0)} · em dinheiro ${money.format(sac.rows[0]?.cashPayment || 0)}`;
        document.getElementById('priceSummary').textContent =
            `${price.duration} parcelas · primeira total ${money.format(price.rows[0]?.installment || 0)} · em dinheiro ${money.format(price.rows[0]?.cashPayment || 0)}`;
        const difference = roundMoney(Math.abs(sac.totalInterest - price.totalInterest));
        const lowerCost = sac.totalInterest <= price.totalInterest ? 'SAC' : 'PRICE';
        document.getElementById('interestDifference').textContent = money.format(difference);
        document.getElementById('differenceSummary').textContent =
            difference === 0 ? 'Mesmo total de juros' : `Menos juros no ${lowerCost}`;
        document.getElementById('sacTotalPaid').textContent = money.format(sac.totalPaid);
        document.getElementById('priceTotalPaid').textContent = money.format(price.totalPaid);
        const paymentDifference = roundMoney(Math.abs(sac.totalPaid - price.totalPaid));
        const lowerPayment = sac.totalPaid <= price.totalPaid ? 'SAC' : 'PRICE';
        document.getElementById('paymentDifference').textContent = money.format(paymentDifference);
        document.getElementById('paymentDifferenceSummary').textContent =
            paymentDifference === 0 ? 'Mesmo total de pagamentos' : `Menor total no ${lowerPayment}`;
        [
            ['projectionMonthsSac', `${number.format(sac.duration)} meses`],
            ['projectionMonthsPrice', `${number.format(price.duration)} meses`],
            ['selicAnnualRateSac', `${percentage.format(state.selicRate)}%`],
            ['selicAnnualRatePrice', `${percentage.format(state.selicRate)}%`],
            ['selicAccumulatedRateSac', `${percentage.format(selicProjection.SAC.accumulatedRate)}%`],
            ['selicAccumulatedRatePrice', `${percentage.format(selicProjection.PRICE.accumulatedRate)}%`],
            ['selicProjectedAmountSac', money.format(selicProjection.SAC.projectedAmount)],
            ['selicProjectedAmountPrice', money.format(selicProjection.PRICE.projectedAmount)],
            ['ipcaAnnualRateSac', `${percentage.format(state.ipcaRate)}%`],
            ['ipcaAnnualRatePrice', `${percentage.format(state.ipcaRate)}%`],
            ['ipcaAccumulatedRateSac', `${percentage.format(ipcaProjection.SAC.accumulatedRate)}%`],
            ['ipcaAccumulatedRatePrice', `${percentage.format(ipcaProjection.PRICE.accumulatedRate)}%`],
            ['ipcaProjectedAmountSac', money.format(ipcaProjection.SAC.projectedAmount)],
            ['ipcaProjectedAmountPrice', money.format(ipcaProjection.PRICE.projectedAmount)],
            ['propertyInitialValueSac', money.format(state.propertyValue)],
            ['propertyInitialValuePrice', money.format(state.propertyValue)],
            ['propertyAnnualRateSac', `${percentage.format(state.propertyAppreciationRate)}%`],
            ['propertyAnnualRatePrice', `${percentage.format(state.propertyAppreciationRate)}%`],
            ['propertyAccumulatedRateSac', `${percentage.format(propertyProjection.SAC.accumulatedRate)}%`],
            ['propertyAccumulatedRatePrice', `${percentage.format(propertyProjection.PRICE.accumulatedRate)}%`],
            ['propertyAppreciationSac', money.format(propertyProjection.SAC.accumulatedAmount)],
            ['propertyAppreciationPrice', money.format(propertyProjection.PRICE.accumulatedAmount)],
            ['propertyProjectedValueSac', money.format(propertyProjection.SAC.projectedAmount)],
            ['propertyProjectedValuePrice', money.format(propertyProjection.PRICE.projectedAmount)]
        ].forEach(([id, value]) => {
            document.getElementById(id).textContent = value;
        });
        [
            ['selicVsSacInterest', selicProjection.SAC.accumulatedAmount - sac.totalInterest],
            ['selicVsPriceInterest', selicProjection.PRICE.accumulatedAmount - price.totalInterest],
            ['selicVsSacPayments', selicProjection.SAC.projectedAmount - sac.totalPaid],
            ['selicVsPricePayments', selicProjection.PRICE.projectedAmount - price.totalPaid],
            ['ipcaVsSacInterest', ipcaProjection.SAC.accumulatedAmount - sac.totalInterest],
            ['ipcaVsPriceInterest', ipcaProjection.PRICE.accumulatedAmount - price.totalInterest],
            ['ipcaVsSacPayments', ipcaProjection.SAC.projectedAmount - sac.totalPaid],
            ['ipcaVsPricePayments', ipcaProjection.PRICE.projectedAmount - price.totalPaid]
        ].forEach(([id, difference]) => {
            document.getElementById(id).textContent = formatDifference(roundMoney(difference));
        });
        [
            ['sac', sac],
            ['price', price]
        ].forEach(([prefix, schedule]) => {
            document.getElementById(`${prefix}TrTotal`).textContent = money.format(schedule.totalTr);
            document.getElementById(`${prefix}InsuranceTotal`).textContent = money.format(schedule.totalInsurance);
            document.getElementById(`${prefix}AdminTotal`).textContent = money.format(schedule.totalAdmin);
            document.getElementById(`${prefix}FgtsDeposits`).textContent = money.format(schedule.totalFgtsDeposits);
            document.getElementById(`${prefix}FgtsYield`).textContent = money.format(schedule.totalFgtsYield);
            document.getElementById(`${prefix}FgtsUsed`).textContent = money.format(schedule.totalFgtsUsed);
            document.getElementById(`${prefix}FgtsRemaining`).textContent = money.format(schedule.remainingFgts);
        });

        const lastMonth = Math.max(sac.duration, price.duration);
        const chartValues = (schedule, field, terminalValue = 0) =>
            Array.from({length: lastMonth}, (_, index) =>
                schedule.rows[index] ? schedule.rows[index][field] : terminalValue
            );
        drawChart('monthlyInterestChart', [
            {className: 'financing-chart-line-sac', values: chartValues(sac, 'interest')},
            {className: 'financing-chart-line-price', values: chartValues(price, 'interest')}
        ], 'Evolução dos juros pagos por parcela nos sistemas SAC e PRICE.');
        drawChart('cumulativeInterestChart', [
            {className: 'financing-chart-line-sac', values: chartValues(sac, 'cumulativeInterest', sac.totalInterest)},
            {className: 'financing-chart-line-price', values: chartValues(price, 'cumulativeInterest', price.totalInterest)}
        ], 'Comparação dos juros acumulados nos sistemas SAC e PRICE.');
        drawChart('cumulativePaymentsChart', [
            {className: 'financing-chart-line-sac', values: chartValues(sac, 'cumulativePaid', sac.totalPaid)},
            {className: 'financing-chart-line-price', values: chartValues(price, 'cumulativePaid', price.totalPaid)}
        ], 'Comparação do total de pagamentos acumulados nos sistemas SAC e PRICE.');
        drawChart('installmentDeviationChart', [
            {
                className: 'financing-chart-line-sac',
                values: sac.rows.map(row => roundMoney(row.installment - sac.firstInstallment))
            },
            {
                className: 'financing-chart-line-price',
                values: price.rows.map(row => roundMoney(row.installment - price.firstInstallment))
            }
        ], 'Variação da prestação total de cada mês em relação à primeira parcela do sistema.');
        drawChart('outstandingBalanceChart', [
            {className: 'financing-chart-line-sac', values: chartValues(sac, 'closingBalance')},
            {className: 'financing-chart-line-price', values: chartValues(price, 'closingBalance')}
        ], 'Evolução do saldo devedor nos sistemas SAC e PRICE.');
        drawChart('cumulativeTrChart', [
            {className: 'financing-chart-line-sac', values: chartValues(sac, 'cumulativeTr', sac.totalTr)},
            {className: 'financing-chart-line-price', values: chartValues(price, 'cumulativeTr', price.totalTr)}
        ], 'Comparação da correção acumulada pela TR nos sistemas SAC e PRICE.');
        drawChart('monthlyChargesChart', [
            {
                className: 'financing-chart-line-sac',
                values: sac.rows.map(row => roundMoney(row.mip + row.dfi + row.adminFee))
            },
            {
                className: 'financing-chart-line-price',
                values: price.rows.map(row => roundMoney(row.mip + row.dfi + row.adminFee))
            }
        ], 'Comparação mensal dos custos estimados de seguros MIP e DFI e da taxa administrativa nos sistemas SAC e PRICE.');
        drawChart('fgtsBalanceChart', [
            {className: 'financing-chart-line-sac', values: chartValues(sac, 'cumulativeFgtsUsed', sac.totalFgtsUsed)},
            {className: 'financing-chart-line-price', values: chartValues(price, 'cumulativeFgtsUsed', price.totalFgtsUsed)}
        ], 'Soma acumulada dos valores pagos pelo FGTS nos sistemas SAC e PRICE.');
        document.getElementById('scheduleDescription').textContent =
            `Exibindo ${activeSystem}: ${schedules[activeSystem].duration} parcelas, com abatimentos aplicados após cada parcela.`;
        document.getElementById('schedulePanel').setAttribute(
            'aria-labelledby', activeSystem === 'SAC' ? 'sacTab' : 'priceTab'
        );
    }

    function renderSchedule() {
        const body = document.getElementById('scheduleRows');
        const fragment = document.createDocumentFragment();
        schedules[activeSystem].rows.forEach(row => {
            const tr = document.createElement('tr');
            const values = [
                String(row.month),
                money.format(row.openingBalance),
                money.format(row.trCorrection),
                money.format(row.installment),
                money.format(row.interest),
                money.format(row.amortization),
                String(row.age),
                number.format(row.mipRate),
                money.format(row.mip),
                money.format(row.dfi),
                money.format(row.adminFee),
                money.format(row.monthlyExtra)
            ];
            values.forEach((value, index) => {
                const cell = document.createElement('td');
                cell.textContent = value;
                if (index === 0) cell.className = 'financing-month-cell';
                tr.append(cell);
            });
            const extraCell = document.createElement('td');
            const extraInput = document.createElement('input');
            extraInput.type = 'number';
            extraInput.min = '0';
            extraInput.max = '1000000000';
            extraInput.step = '100';
            extraInput.inputMode = 'decimal';
            extraInput.value = state.extraByMonth[row.month] || 0;
            extraInput.className = 'financing-extra-input';
            extraInput.setAttribute('aria-label', `Amortização extra pontual do saldo devedor na parcela ${row.month}, em reais`);
            extraInput.dataset.month = String(row.month);
            extraCell.append(extraInput);
            tr.append(extraCell);
            [
                money.format(row.fgtsDeposit),
                money.format(row.fgtsYield)
            ].forEach(value => {
                const cell = document.createElement('td');
                cell.textContent = value;
                tr.append(cell);
            });
            const fgtsInstallmentCell = document.createElement('td');
            fgtsInstallmentCell.textContent = money.format(row.fgtsForInstallment);
            const fgtsReserveCell = document.createElement('td');
            fgtsReserveCell.textContent = money.format(row.fgtsInstallmentReserve);
            const fgtsAmortizationCell = document.createElement('td');
            fgtsAmortizationCell.textContent = money.format(row.fgtsForAmortization);
            const cashPaymentCell = document.createElement('td');
            cashPaymentCell.textContent = money.format(row.cashPayment);
            const closingCell = document.createElement('td');
            closingCell.textContent = money.format(row.closingBalance);
            const fgtsBalanceCell = document.createElement('td');
            fgtsBalanceCell.textContent = money.format(row.fgtsBalance);
            tr.append(fgtsInstallmentCell, fgtsReserveCell, fgtsAmortizationCell,
                cashPaymentCell, closingCell, fgtsBalanceCell);
            fragment.append(tr);
        });
        body.replaceChildren(fragment);
        body.querySelectorAll('.financing-extra-input').forEach(input => {
            input.addEventListener('change', () => {
                const month = input.dataset.month;
                const extra = Number(input.value);
                if (!Number.isFinite(extra) || extra < 0 || extra > 1000000000) {
                    showMessage('O abatimento pontual deve ser um valor positivo ou zero.', true);
                    input.value = state.extraByMonth[month] || 0;
                    return;
                }
                if (extra === 0) delete state.extraByMonth[month];
                else state.extraByMonth[month] = extra;
                markCalculationDirty();
            });
        });
        const fgtsStrategyField = document.getElementById('fgtsStrategyField');
        fgtsStrategyField.hidden = fields.fgtsMode.value !== 'amortize';
    }

    function recalculate(renderRows = true) {
        try {
            readInputs();
            schedules = {SAC: calculateSchedule('SAC'), PRICE: calculateSchedule('PRICE')};
            document.getElementById('comparison').hidden = false;
            updateSummaries();
            if (renderRows) renderSchedule();
            const saved = saveState();
            calculationDirty = false;
            document.getElementById('calculationStatus').textContent = saved
                ? 'Simulação, taxas e valorização do imóvel recalculadas e salvas neste navegador.'
                : 'Simulação, taxas e valorização do imóvel recalculadas, mas não foi possível salvar neste navegador.';
            if (storageWarning) {
                showMessage('Simulação carregada com valores padrão: os dados salvos estavam inválidos.', true);
                storageWarning = false;
            }
            else if (saved) showMessage('Simulação atualizada e salva neste navegador.');
        } catch (error) {
            document.getElementById('comparison').hidden = true;
            showMessage(error.message, true);
        }
    }

    function exportCsv() {
        const headings = [
            'Sistema', 'Parcela', 'Saldo inicial', 'Correção TR', 'Prestação',
            'Juros', 'Amortização', 'Idade', 'Taxa MIP (%)', 'MIP', 'DFI', 'Taxa administrativa',
            'Extra mensal', 'Extra pontual', 'Depósito FGTS', 'Rendimento FGTS',
            'FGTS em parcelas', 'FGTS reservado futuro', 'FGTS em amortização',
            'Pago em dinheiro', 'Saldo devedor', 'Saldo FGTS'
        ];
        const lines = [headings.join(';')];
        ['SAC', 'PRICE'].forEach(system => {
            schedules[system].rows.forEach(row => {
                lines.push([
                    system, row.month, row.openingBalance.toFixed(2), row.trCorrection.toFixed(2),
                    row.installment.toFixed(2), row.interest.toFixed(2),
                    row.amortization.toFixed(2), row.age, row.mipRate.toFixed(3),
                    row.mip.toFixed(2), row.dfi.toFixed(2),
                    row.adminFee.toFixed(2), row.monthlyExtra.toFixed(2), row.oneTimeExtra.toFixed(2),
                    row.fgtsDeposit.toFixed(2), row.fgtsYield.toFixed(2),
                    row.fgtsForInstallment.toFixed(2), row.fgtsInstallmentReserve.toFixed(2),
                    row.fgtsForAmortization.toFixed(2),
                    row.cashPayment.toFixed(2), row.closingBalance.toFixed(2), row.fgtsBalance.toFixed(2)
                ].join(';'));
            });
        });
        const blob = new Blob(['\uFEFF', lines.join('\r\n')], {type: 'text/csv;charset=utf-8'});
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'cronogramas-financiamento.csv';
        link.click();
        URL.revokeObjectURL(url);
    }

    loadState();
    fields.fgtsMode.addEventListener('change', () => {
        document.getElementById('fgtsStrategyField').hidden = fields.fgtsMode.value !== 'amortize';
        markCalculationDirty();
    });
    Object.values(fields).forEach(field => {
        field.addEventListener('input', markCalculationDirty);
        field.addEventListener('change', markCalculationDirty);
    });
    document.getElementById('calculateWithAbatements').addEventListener('click', () => recalculate());
    document.querySelectorAll('.financing-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            activeSystem = tab.dataset.system;
            document.querySelectorAll('.financing-tab').forEach(item => {
                const selected = item === tab;
                item.classList.toggle('is-active', selected);
                item.setAttribute('aria-selected', String(selected));
            });
            renderSchedule();
            updateSummaries();
        });
    });
    document.getElementById('downloadSchedule').addEventListener('click', exportCsv);
    recalculate();
})();
