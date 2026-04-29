import { useEffect } from "react";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { Button } from "@/components/common/Button/Button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/common/Card/Card";
import { Badge } from "@/components/common/Badge/Badge";
import type { JSX } from "react/jsx-runtime";
import Header from "@/components/common/Header/Header";
import Footer from "@/components/common/Footer/Footer";
import { useMsalLoginLogoutSignup } from "@/hooks/auth/useMsalLoginLogoutSignup";
import { blogPosts, mainPosts } from "../data";
import { brandStyles } from "@/lib/design/styles";
import { useParams, Link } from "@tanstack/react-router";

type ContentItem = {
  type: string;
  level?: number;
  text?: string;
  items?: string[];
};

const renderContent = (contentItem: ContentItem, index: number) => {
  switch (contentItem.type) {
    case "heading": {
      const HeadingTag = `h${contentItem.level}` as keyof JSX.IntrinsicElements;
      const headingClasses = {
        2: "text-3xl font-bold text-gray-900 mt-12 mb-6 first:mt-0",
        3: "text-2xl font-semibold text-gray-900 mt-10 mb-4",
        4: "text-xl font-semibold text-gray-900 mt-8 mb-3",
      };
      return (
        <HeadingTag
          key={index}
          className={
            headingClasses[contentItem.level as keyof typeof headingClasses] ||
            headingClasses[2]
          }
        >
          {contentItem.text}
        </HeadingTag>
      );
    }
    case "paragraph":
      return (
        <p
          key={index}
          className="text-lg text-justify leading-relaxed text-gray-700 mb-6"
        >
          {contentItem.text}
        </p>
      );

    case "list":
      return (
        <ul key={index} className="space-y-3 mb-6 ml-6">
          {(contentItem.items ?? []).map((item: string, itemIndex: number) => (
            <li key={itemIndex} className="flex items-start space-x-3">
              <div className="w-2 h-2 bg-orange-400 rounded-full mt-3 shrink-0"></div>
              <span className="text-gray-700 leading-relaxed">{item}</span>
            </li>
          ))}
        </ul>
      );

    case "ordered-list":
      return (
        <ol key={index} className="space-y-3 mb-6 ml-6">
          {(contentItem.items ?? []).map((item: string, itemIndex: number) => (
            <li key={itemIndex} className="flex items-start space-x-3">
              <div className="w-6 h-6 bg-orange-400 text-white rounded-full flex items-center justify-center text-sm font-semibold shrink-0 mt-1">
                {itemIndex + 1}
              </div>
              <span className="text-gray-700 leading-relaxed">{item}</span>
            </li>
          ))}
        </ol>
      );

    default:
      return null;
  }
};

export default function BlogPostPage() {
  const { slug } = useParams({ from: "/blog_/$slug" });
  const post = mainPosts[slug as keyof typeof mainPosts];
  const { signUp } = useMsalLoginLogoutSignup();
  const relatedPosts = blogPosts.filter((p) => p.slug !== slug).slice(0, 3);

  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  if (!post) {
    return (
      <div
        className={`min-h-screen flex items-center justify-center ${brandStyles.page}`}
      >
        <div className="text-center">
          <h1 className="text-4xl font-bold text-gray-900 mb-4">
            Post Not Found
          </h1>
          <p className="text-gray-600 mb-8">
            The blog post you're looking for doesn't exist.
          </p>
          <Button
            asChild
            className={`bg-orange-500 hover:bg-orange-600 ${brandStyles.brandButton}`}
          >
            <Link to="/blog">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back to Blog
            </Link>
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className={`min-h-screen bg-gray-50 ${brandStyles.page}`}>
      {/* Header */}
      <Header text="Back to Blog" backRoute="/blog" />
      {/* Article Header */}
      <section className="container mx-auto px-4 pb-8 pt-16">
        <div className="max-w-4xl mx-auto">
          <div className="mb-6">
            <Badge
              className={`mb-4 ${brandStyles.brandOutlineButton} ${brandStyles.surface}`}
            >
              {post.category}
            </Badge>
            <h1 className="text-4xl md:text-5xl font-bold text-gray-900 mb-6 leading-tight">
              {post.title}
            </h1>
            <p className="text-xl text-justify text-gray-600 mb-8 leading-relaxed">
              {post.excerpt}
            </p>
          </div>

          {/* Article Meta */}
          <div className="flex flex-wrap items-center justify-between mb-8 pb-8 border-b border-gray-200"></div>

          {/* Featured Image */}
          <div className="mb-12">
            <img
              src={post.image || "/placeholder.svg"}
              alt={post.title}
              width={800}
              height={400}
              className="w-full h-64 md:h-96 object-cover rounded-lg"
            />
          </div>
        </div>
      </section>

      {/* Article Content */}
      <section className="container mx-auto px-4 pb-16">
        <div className="max-w-4xl mx-auto">
          <div className="max-w-none">
            {post.content.map((contentItem, index) =>
              renderContent(contentItem, index),
            )}
          </div>

          {/* Tags */}
          <div className="mt-12 pt-8 border-t border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Tags</h3>
            <div className="flex flex-wrap gap-2">
              {post.tags.map((tag) => (
                <Badge
                  key={tag}
                  variant="outline"
                  className="border-gray-300 text-orange-600 hover:bg-orange-50"
                >
                  {tag}
                </Badge>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Related Posts */}
      <section className="bg-gray-50 py-16">
        <div className="container mx-auto px-4">
          <div className="max-w-4xl mx-auto">
            <h3 className="text-3xl font-bold text-gray-900 mb-8">
              Other Articles
            </h3>
            <div className="grid md:grid-cols-3 gap-6">
              {relatedPosts.map((relatedPost) => (
                <Card
                  key={relatedPost.slug}
                  className={`hover:shadow-lg transition-shadow duration-300 ${brandStyles.surface}`}
                >
                  <CardHeader>
                    <Badge
                      className={`w-fit mb-2 bg-orange-500 text-white ${brandStyles.brandBg}`}
                    >
                      {relatedPost.category}
                    </Badge>
                    <CardTitle className="text-lg">
                      <Link
                        to="/blog/$slug"
                        params={{ slug: relatedPost.slug }}
                        className="hover:underline"
                      >
                        {relatedPost.title}
                      </Link>
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <Button
                      variant="outline"
                      asChild
                      size="sm"
                      className={`bg-transparent border-orange-500 text-orange-600 hover:bg-orange-50 ${brandStyles.brandOutlineButton}`}
                    >
                      <Link
                        to="/blog/$slug"
                        params={{ slug: relatedPost.slug }}
                      >
                        Read More
                        <ArrowRight className="ml-2 h-4 w-4" />
                      </Link>
                    </Button>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="container mx-auto px-4 py-16 text-center">
        <div className="max-w-2xl mx-auto">
          <h3 className="text-3xl font-bold text-gray-900 mb-4">
            Need Help with Your Documents?
          </h3>
          <p className="text-xl text-gray-600 mb-8">
            Let Admina analyze and translate your German bureaucratic documents
            in minutes.
          </p>
          <Button
            size="lg"
            variant="outline"
            className={`text-xl px-12 py-6 rounded-full border-2px transition-all duration-500 bg-transparent transform hover:scale-110 hover:-translate-y-1 ${brandStyles.brandOutlineButton}`}
            onClick={signUp}
          >
            Try Admina Free
            <ArrowRight className="ml-3 h-6 w-6" />
          </Button>
        </div>
      </section>

      {/* Footer */}
      <Footer />
    </div>
  );
}
