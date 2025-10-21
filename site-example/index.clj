[:html
 [:head
  [:link {:rel "stylesheet" :type "text/css" :href "css/style.css"}]
  [:title "defblog example"]]
 [:body
  [:h3 "Recent posts:"]
  #posts/list {:ul-attrs {:class "posts"} :item-attrs {:class "post-item"}}]]
