# defblog 🌀

**defblog** is a minimalist, hackable **static site generator** written in [Babashka](https://babashka.org/).  
It’s designed for people who love **Clojure**, want full control over their content, and don’t need the bloat of giant frameworks.

👉 You write posts in **Hiccup**.  
👉 defblog turns them into clean, fast, static HTML.  
👉 No magic, no lock-in — just plain Clojure data and Babashka scripts.

---

## ✨ Features

- ⚡ **Fast builds** – Powered by Babashka (instant startup, no JVM overhead).
- 📝 **Flexible content** – Write posts in Hiccup (`.clj`/`.edn` files with Clojure data structures).
- 🗂 **Organized** – Website files live in a `site/` directory. A `site-example/` directory is provided as a starting point.
- 🔊 **Blogging** – Posts live in a `site/posts/` directory.
- 🔗 **Automatic index** – Generates a post listing page with links to your content.
- 🎨 **Customizable templates** – Tweak layouts using Hiccup2, your CSS, and your logic.
- 🛠 **Hackable scripts** – Everything is plain Clojure; easy to modify and extend.
- 🌐 **No server required** – Deploy anywhere: GitHub Pages, Netlify, your own server.

---

## 📦 Installation

Clone the repository:

```bash
git clone https://github.com/your-username/defblog.git
cd defblog
```

Make sure you have [Babashka](https://github.com/babashka/babashka#installation) installed:

```bash
bb --version
```

If you want to run the dev server locally to see live updates in your browser without having to refresh, also make sure
you have Node.js installed:

```bash
npx --version
```

---

## 🚀 Usage

### Start a new site
```bash
cp -r site-example site
```

### Build your site
```bash
bb build
```

This compiles everything in `site/` into static HTML inside `publish/`.

### Preview locally
```bash
bb dev
```

Then open [http://localhost:4000](http://localhost:4000).

While the server is running, modifying files in `site/` will trigger a build, and the browser will automatically refresh
to show the updated content.

---

## 📝 Writing Posts

Drop a file like this into `site/posts/`:

```clojure
^{:title "My first post"
  :date "2025-10-20"
  :published true}

[:article
 [:h1 "My First Post"]
 [:p "Hello world! This is a post written in Hiccup."]]
```

If you don't include a `:date` in the file metadata, defblog will try to get it from either
the filename (e.g. `20251020-my-first-post.clj`), or from the file creation date.

Likewise, if you don't include a `:title` in the file metadata, defblog will try to 
create one from the file name.

If you don't include a `:published` in the file metadata, or `:published` is falsy, defblog will not show your post
in the post index.

---

## 🧩 Project Layout

```
defblog/
├── bb.edn             # Babashka deps/tasks
├── deps.edn           # Clojure deps
├── publish/           # Generated HTML output
├── scripts/           # Scripts for building, running the dev server, etc
    ├── build.bb       # Build script
    ├── dev.bb         # Dev helper script (live reload, WebSocket refresh)
├── site-example/      # Starter example site
├── site/              # Your site (copy from site-example)
└── src/               # Namespaces for useful code
```

---

## 🤝 Contributing

Contributions are welcome!  
Whether it’s bug reports, new features, or better docs — open a PR or issue and join the fun.

---

## 📜 License

MIT License © 2025 Your Name

---

> 💡 **Philosophy:** defblog is not about being the biggest or most feature-packed static site generator.  
> It’s about being **simple, hackable, and Clojure-native** — a static site generator you can actually read, understand, and extend.