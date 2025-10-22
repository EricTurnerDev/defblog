[:html
 [:head
  [:link {:rel "stylesheet" :type "text/css" :href "css/style.css"}]
  [:title "defblog example"]]
 [:body
  [:h2 "Posts"]
  #posts/list {:ul-attrs {:class "posts"} :item-attrs {:class "post-item"}}]]
