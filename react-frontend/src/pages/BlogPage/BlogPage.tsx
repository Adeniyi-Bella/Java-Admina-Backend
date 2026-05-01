import { ArrowRight } from "lucide-react";
import Footer from "@/components/common/Footer/Footer";
import Header from "@/components/common/Header/Header";
import { Button } from "@/components/common/Button/Button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/common/Card/Card";
import { Badge } from "@/components/common/badge";
import { brandStyles } from "@/lib/design/styles";
import { blogPosts } from "./data";
import { Link, useSearch } from "@tanstack/react-router";

const categories = [
  "All",
  ...Array.from(new Set(blogPosts.map((post) => post.category))),
];

export default function BlogPage() {
  const search = useSearch({ from: "/blog" });
  const rawCategory = (search as { category?: string }).category;
  const selectedCategory =
    typeof rawCategory === "string" && categories.includes(rawCategory)
      ? rawCategory
      : "All";

  const filteredPosts =
    selectedCategory === "All"
      ? blogPosts
      : blogPosts.filter((post) => post.category === selectedCategory);

  const featuredPost =
    filteredPosts.length > 0 ? filteredPosts[0] : blogPosts[0];
  const gridPosts = filteredPosts.slice(1);

  return (
    <div className={`min-h-screen ${brandStyles.surface}`}>
      <Header text="Back To Home" backRoute="/" />

      <section className="py-20">
        <div className="container mx-auto px-4 text-center">
          <h1 className="text-5xl font-bold text-gray-900 mb-6">Admina Blog</h1>
          <p className="text-xl text-gray-600 max-w-3xl mx-auto mb-8">
            Expert insights, guides, and tips for navigating German bureaucracy.
            Stay informed and make sense of official documents with our
            comprehensive resources.
          </p>
        </div>
      </section>

      <section className="container mx-auto px-4 mb-12">
        <div className="flex flex-wrap justify-center gap-4">
          {categories.map((category) => {
            const isActive = category === selectedCategory;
            return (
              <Button
                key={category}
                variant="outline"
                className={`${isActive ? brandStyles.brandBg + " text-white" : brandStyles.brandOutlineButton} ${!isActive ? "hover:bg-opacity-10" : ""}`}
                asChild
              >
                <Link
                  to="/blog"
                  search={category === "All" ? {} : { category }}
                >
                  {category}
                  {category !== "All" && (
                    <Badge
                      variant="secondary"
                      className={`ml-2 text-xs ${isActive ? "bg-white/20 text-white" : `${brandStyles.brandBg} text-white`}`}
                    >
                      {
                        blogPosts.filter((post) => post.category === category)
                          .length
                      }
                    </Badge>
                  )}
                </Link>
              </Button>
            );
          })}
        </div>
      </section>

      {filteredPosts.length > 0 && (
        <section className="container mx-auto px-4 mb-16">
          <div className="max-w-4xl mx-auto">
            <Card
              className={`overflow-hidden hover:shadow-lg transition-shadow duration-300 ${brandStyles.page}`}
            >
              <div className="md:flex">
                <div className="md:w-1/2">
                  <img
                    src={featuredPost.image || "/placeholder.svg"}
                    alt={featuredPost.title}
                    width={600}
                    height={300}
                    className="w-full h-64 md:h-full object-cover"
                  />
                </div>
                <div className="md:w-1/2 p-8">
                  <div className="flex items-center space-x-4 mb-4">
                    <Badge className={`${brandStyles.brandBg} text-white`}>
                      Featured
                    </Badge>
                    <Badge
                      variant="outline"
                      className={brandStyles.brandOutlineButton}
                    >
                      {featuredPost.category}
                    </Badge>
                  </div>
                  <h2 className="text-2xl font-bold text-gray-900 mb-4">
                    {featuredPost.title}
                  </h2>
                  <p className="text-gray-600 mb-6">{featuredPost.excerpt}</p>
                  <Button asChild className={brandStyles.brandButton}>
                    <Link to="/blog/$slug" params={{ slug: featuredPost.slug }}>
                      Read More
                      <ArrowRight className="ml-2 h-4 w-4" />
                    </Link>
                  </Button>
                </div>
              </div>
            </Card>
          </div>
        </section>
      )}

      <section className="container mx-auto px-4 pb-20">
        {gridPosts.length > 0 ? (
          <>
            <h2 className="text-3xl font-bold text-gray-900 mb-12 text-center">
              {selectedCategory === "All"
                ? "Latest Articles"
                : `More in ${selectedCategory}`}
            </h2>
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
              {gridPosts.map((post) => (
                <Card
                  key={post.slug}
                  className={`overflow-hidden hover:shadow-lg transition-shadow duration-300 ${brandStyles.page}`}
                >
                  <div className="relative">
                    <img
                      src={post.image || "/placeholder.svg"}
                      alt={post.title}
                      width={400}
                      height={200}
                      className="w-full h-48 object-cover"
                    />
                    <Badge
                      className={`absolute top-4 left-4 text-white ${brandStyles.brandBg}`}
                    >
                      {post.category}
                    </Badge>
                  </div>
                  <CardHeader>
                    <CardTitle className="text-xl line-clamp-2">
                      {post.title}
                    </CardTitle>
                    <CardDescription className="line-clamp-3">
                      {post.excerpt}
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <Button
                      variant="outline"
                      asChild
                      className={brandStyles.brandOutlineButton}
                    >
                      <Link to="/blog/$slug" params={{ slug: post.slug }}>
                        Read More
                        <ArrowRight className="ml-2 h-4 w-4" />
                      </Link>
                    </Button>
                  </CardContent>
                </Card>
              ))}
            </div>
          </>
        ) : (
          <div className="text-center py-16">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">
              No articles found
            </h2>
            <p className="text-gray-600 mb-8">
              There are no articles in the {selectedCategory} category yet.
            </p>
            <Button asChild className={brandStyles.brandButton}>
              <Link to="/blog">View All Articles</Link>
            </Button>
          </div>
        )}
      </section>

      <Footer />
    </div>
  );
}
