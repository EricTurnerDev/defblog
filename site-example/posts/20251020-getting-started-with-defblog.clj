^{:title "Getting started with defblog"
  :date "2025-10-21"
  :published true}
[:article
 [:h1 "Getting started with defblog"]
 [:p "defblog is a static site generator designed for people who want a minimalistic tool for writing and publishing a
  simple website."]
 [:h2 "Features"]
 [:ul
  [:li "Easy to install and start using."]
  [:li "Stable, requiring few dependencies."]
  [:li "Write pages in " [:a {:href "https://weavejester.github.io/hiccup/syntax.html"} "hiccup"] "."]
  [:li "Generates a static website that can be hosted anywhere."]
  [:li "Run in development mode while writing to see your changes in a browser without needing to refresh or restart anything."]]
 [:h2 "Installation"]
 [:p "Install " [:a {:href "https://github.com/babashka/babashka#installation"} "Babashka"] "."]
 [:p "Install " [:a {:href "https://docs.npmjs.com/downloading-and-installing-node-js-and-npm"} "Node.js"]
  " (only necessary if you want to run defblog in development mode). I strongly recommend using a Node version manager to install Node.js."]
 [:p "Clone the git repository. You are now ready to build and view the example website, and start creating your own website."]
 [:h2 "Running"]
 [:p "Start by building the example website included with defblog. Change into the defblog directory and run \"bb build-example\".
  This will process the files in the site-example/ directory, and output them into the publish/ directory. You can open the publish/index.html
  file in a web browser."]
 [:p "When you start working on your own site, your files will be stored under the site/ directory rather than the site-example/ directory. In this case,
  you will run \"bb build\" instead of \"bb build-example\"."]
 [:p "Alternatively, you can run defblog in development mode (this is my preferred way of using defblog). Development mode
  starts a small local web server, and automatically updates the website files in the publish/ directory whenever files in the site-example/
  or site/ directories are changed. Run \"bb dev-example\" or \"bb dev\", and open your browser to the URL displayed in the terminal. Keep in mind that
  running in development mode requires Node.js to be installed."]
 [:h2 "Customizing Your Website"]
 [:p "Start by copying all the files from the site-example/ directory into the site/ directory, then edit those files."]]