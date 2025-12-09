'use client';

import { useState } from 'react';
import { Check, Copy } from 'lucide-react';
import { cn } from '@/lib/utils';

interface CodeBlockProps {
    code: string;
    language?: string;
    filename?: string;
    showLineNumbers?: boolean;
}

export function CodeBlock({
    code,
    language = 'bash',
    filename,
    showLineNumbers = false,
}: CodeBlockProps) {
    const [copied, setCopied] = useState(false);

    const copyToClipboard = async () => {
        await navigator.clipboard.writeText(code);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const lines = code.split('\n');

    return (
        <div className="relative group rounded-xl overflow-hidden border border-border/50 bg-black/40 my-6">
            {/* Header */}
            {(filename || language) && (
                <div className="flex items-center justify-between px-4 py-2 border-b border-border/50 bg-black/20">
                    <div className="flex items-center gap-2">
                        <div className="flex gap-1.5">
                            <div className="w-3 h-3 rounded-full bg-error/60" />
                            <div className="w-3 h-3 rounded-full bg-warning/60" />
                            <div className="w-3 h-3 rounded-full bg-success/60" />
                        </div>
                        {filename && (
                            <span className="ml-2 text-sm text-muted-foreground font-mono">
                                {filename}
                            </span>
                        )}
                    </div>
                    <span className="text-xs text-muted-foreground uppercase tracking-wide">
                        {language}
                    </span>
                </div>
            )}

            {/* Code */}
            <div className="relative">
                <button
                    onClick={copyToClipboard}
                    className={cn(
                        'absolute right-3 top-3 p-2 rounded-lg transition-all duration-200',
                        'bg-white/5 hover:bg-white/10 text-muted-foreground hover:text-foreground',
                        'opacity-0 group-hover:opacity-100 focus:opacity-100',
                        copied && 'bg-success/20 text-success'
                    )}
                    aria-label="Copy code"
                >
                    {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                </button>

                <pre className="overflow-x-auto p-4 text-sm">
                    <code className="font-mono text-foreground/90">
                        {showLineNumbers ? (
                            <table className="border-collapse">
                                <tbody>
                                    {lines.map((line, i) => (
                                        <tr key={i} className="hover:bg-white/5">
                                            <td className="pr-4 text-muted-foreground/50 select-none text-right w-8">
                                                {i + 1}
                                            </td>
                                            <td className="whitespace-pre">{line}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        ) : (
                            code
                        )}
                    </code>
                </pre>
            </div>
        </div>
    );
}
