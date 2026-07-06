import puppeteer from 'puppeteer-extra';
import StealthPlugin from 'puppeteer-extra-plugin-stealth';
import express from 'express';

puppeteer.use(StealthPlugin());

const PORT = parseInt(process.env.PORT || '3000', 10);
const NAV_TIMEOUT = parseInt(process.env.NAV_TIMEOUT || '30000', 10);

const app = express();
app.use(express.json());

let browser;

async function getBrowser() {
    if (!browser || !browser.isConnected()) {
        browser = await puppeteer.launch({
            headless: true,
            args: [
                '--no-sandbox',
                '--disable-setuid-sandbox',
                '--disable-gpu',
                '--disable-dev-shm-usage',
                '--disable-software-rasterizer',
                '--no-first-run',
                '--disable-blink-features=AutomationControlled',
            ],
        });
    }
    return browser;
}

app.get('/health', (_req, res) => {
    res.json({ status: 'ok', browser: browser?.isConnected() ? 'connected' : 'disconnected' });
});

app.post('/fetch', async (req, res) => {
    const { url } = req.body;
    if (!url) {
        return res.status(400).json({ error: 'url is required' });
    }

    const b = await getBrowser();
    const page = await b.newPage();

    try {
        await page.setViewport({ width: 1280, height: 720 });
        await page.setExtraHTTPHeaders({
            'Accept-Language': 'de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7',
        });
        await page.setUserAgent(
            'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36',
        );

        const delay = 1000 + Math.random() * 2000;
        await new Promise((r) => setTimeout(r, delay));

        const response = await page.goto(url, {
            timeout: NAV_TIMEOUT,
            waitUntil: 'networkidle0',
        });

        const status = response.status();
        const content = await page.content();

        res.json({ status, url: response.url(), content });
    } catch (err) {
        res.status(500).json({ error: err.message, stack: err.stack });
    } finally {
        await page.close();
    }
});

async function main() {
    await getBrowser();
    app.listen(PORT, () => {
        console.log(`scraper-worker listening on port ${PORT}`);
    });
}

main().catch((err) => {
    console.error('Failed to start:', err);
    process.exit(1);
});

process.on('SIGTERM', async () => {
    if (browser) await browser.close();
    process.exit(0);
});
